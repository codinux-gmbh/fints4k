package net.dankito.banking.ui.android.authentication

import android.content.Context
import android.util.Base64
import at.favre.lib.crypto.bcrypt.BCrypt
import com.yakivmospan.scytale.Crypto
import com.yakivmospan.scytale.Options
import com.yakivmospan.scytale.Store
import net.dankito.banking.persistence.IBankingPersistence
import net.dankito.banking.ui.android.security.CryptographyManager
import net.dankito.banking.util.ISerializer
import net.dankito.utils.multiplatform.File
import net.dankito.utils.multiplatform.asString
import org.slf4j.LoggerFactory
import javax.crypto.Cipher
import javax.crypto.SecretKey


open class AuthenticationService(
    protected open val applicationContext: Context,
    protected open val biometricAuthenticationService: IBiometricAuthenticationService,
    protected open val persistence: IBankingPersistence,
    protected open val dataFolder: File,
    protected open val serializer: ISerializer,
    protected open val cryptographyManager: CryptographyManager = CryptographyManager()
) {

    companion object {
        private const val AuthenticationSettingsFilename = "a"

        private const val AuthenticationSettingsFileKey = "AuthenticationSettingsFileKey"

        private val AuthenticationSettingsFileKeyPassword = "AuthenticationSettingsFileKeyAuthenticationSettingsFileKeyPassword".toCharArray() // TODO: store in a secure place

        private const val EncryptionKeyName = "BankingAndroidKey"

        private const val DefaultPasswordEncryptionKey = "AnyData" // TODO: store in a secure place

        private val log = LoggerFactory.getLogger(AuthenticationService::class.java)
    }


    open val isBiometricAuthenticationSupported: Boolean
        get() = biometricAuthenticationService.supportsBiometricAuthentication

    open var authenticationType: AuthenticationType = AuthenticationType.None
        protected set

    protected open var encryptionCipherForBiometric: Cipher? = null


    init {
        log.info("Initializing AuthenticationService (at the end database must get opened) ...")

        val settings = loadAuthenticationSettings()

        if (settings == null) { // first app run -> create a default password
            removeAppProtection()
        }
        else {
            authenticationType = settings.type

            if (settings.type == AuthenticationType.None) {
                openDatabase(settings)
            }
        }
    }


    open fun loginUserWithPassword(enteredPassword: CharArray): Boolean {
        if (isCorrectUserPassword(enteredPassword)) {
            loadAuthenticationSettings()?.let { settings ->
                return openDatabase(settings, enteredPassword)
            }
        }

        return false
    }

    open fun isCorrectUserPassword(enteredPassword: CharArray): Boolean {
        loadAuthenticationSettings()?.let { settings ->
            val result = BCrypt.verifyer().verify(enteredPassword, settings.hashedUserPassword)

            return result.verified
        }

        return false
    }

    open fun authenticateUserWithBiometricToSetAsNewAuthenticationMethod(authenticationResult: (AuthenticationResult) -> Unit) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            authenticationResult(AuthenticationResult(false, "Biometric authentication is only supported on Android 6 and above"))
            return
        }

        val cipher = cryptographyManager.getInitializedCipherForEncryption(EncryptionKeyName)

        biometricAuthenticationService.authenticate(cipher) { result ->
            if (result.successful) {
                this.encryptionCipherForBiometric = cipher
            }

            authenticationResult(result)
        }
    }

    open fun loginUserWithBiometric(result: (Boolean) -> Unit) {
        // Biometric authentication is only supported on Android 6 and above
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            result(false)
            return
        }

        loadAuthenticationSettings()?.let { settings ->
            val iv = decodeFromBase64(settings.initializationVector ?: "")

            val cipher = cryptographyManager.getInitializedCipherForDecryption(EncryptionKeyName, iv)
            biometricAuthenticationService.authenticate(cipher) { authenticationResult ->
                if (authenticationResult.successful) {
                    settings.defaultPassword?.let {
                        val encryptedUserPassword = decodeFromBase64(it)
                        val decrypted = cryptographyManager.decryptData(encryptedUserPassword, cipher)

                        result(openDatabase(decrypted))
                    }
                }
                else {
                    result(false)
                }
            }
        }
        ?: run { result(false) }
    }

    protected open fun openDatabase(settings: AuthenticationSettings, userPassword: CharArray? = null): Boolean {
        settings.defaultPassword?.let { encryptedPassword ->
            settings.initializationVector?.let { iv ->
                settings.salt?.let { salt ->
                    val defaultPassword = cryptographyManager.decryptDataWithPbe(decodeFromBase64(encryptedPassword), DefaultPasswordEncryptionKey,
                        decodeFromBase64(iv), decodeFromBase64(salt))

                    if (userPassword != null) {
                        return openDatabase(concatPasswords(userPassword, defaultPassword))
                    }
                    else {
                        return openDatabase(defaultPassword)
                    }
                }
            }
        }

        return false
    }

    protected open fun openDatabase(password: CharArray): Boolean {
        val result = persistence.decryptData(password)

        log.info("Did decrypting data / opening database succeed? $result")

        return result
    }


    open fun setAuthenticationMethodToBiometric() {
        saveNewAuthenticationMethod(AuthenticationType.Biometric, null)
    }

    open fun setAuthenticationMethodToPassword(newPassword: CharArray) {
        saveNewAuthenticationMethod(AuthenticationType.Password, newPassword)
    }

    open fun removeAppProtection() {
        saveNewAuthenticationMethod(AuthenticationType.None, null)
    }


    protected open fun saveNewAuthenticationMethod(type: AuthenticationType, newUserPassword: CharArray?): Boolean {
        val settings = loadOrCreateDefaultAuthenticationSettings()
        val newDefaultPassword = generateRandomPassword()
        var newDatabasePassword = newDefaultPassword

        settings.type = type
        settings.hashedUserPassword = null
        settings.initializationVector = null
        settings.salt = null

        if (type == AuthenticationType.Biometric) {
            encryptionCipherForBiometric?.let { encryptionCipher ->
                val encryptedPassword = cryptographyManager.encryptData(newDefaultPassword, encryptionCipher)
                settings.defaultPassword = encodeToBase64(encryptedPassword)
                settings.initializationVector = encodeToBase64(encryptionCipher.iv)
            }
        }
        else {
            val salt = cryptographyManager.generateRandomBytes(8)
            val (encryptedPassword, iv) = cryptographyManager.encryptDataWithPbe(newDefaultPassword, DefaultPasswordEncryptionKey, salt)
            settings.defaultPassword = encodeToBase64(encryptedPassword)
            settings.initializationVector = encodeToBase64(iv)
            settings.salt = encodeToBase64(salt)

            if (newUserPassword != null) {
                settings.hashedUserPassword = BCrypt.withDefaults().hashToString(12, newUserPassword)
                newDatabasePassword = concatPasswords(newUserPassword, newDefaultPassword)
            }
        }

        if (persistence.changePassword(newDatabasePassword)) {
            if (saveAuthenticationSettings(settings)) {
                this.authenticationType = type
                this.encryptionCipherForBiometric = null

                return true
            }
        }

        return false
    }

    protected open fun concatPasswords(userPassword: CharArray, defaultPassword: CharArray): CharArray {
        val concatenated = StringBuilder(userPassword.size + defaultPassword.size + 1)

        concatenated.append(userPassword)
        concatenated.append("_")
        concatenated.append(defaultPassword)

        return concatenated.toList().toCharArray()
    }

    protected open fun loadOrCreateDefaultAuthenticationSettings(): AuthenticationSettings {
        return loadAuthenticationSettings() ?: AuthenticationSettings(AuthenticationType.None)
    }

    protected open fun loadAuthenticationSettings(): AuthenticationSettings? {
        try {
            val file = File(dataFolder, AuthenticationSettingsFilename)

            if (file.exists()) {
                val (key, crypto) = getAuthenticationSettingsFileKey()
                val encryptedJson = file.readText()

                val json = crypto.decrypt(encryptedJson, key)

                return serializer.deserializeObject(json, AuthenticationSettings::class)
            }
        } catch (e: Exception) {
            log.error("Could not load AuthenticationSettings", e)
        }

        return null
    }

    protected open fun saveAuthenticationSettings(settings: AuthenticationSettings): Boolean {
        try {
            serializer.serializeObjectToString(settings)?.let { json ->
                val (key, crypto) = getAuthenticationSettingsFileKey()
                val encryptedJson = crypto.encrypt(json, key)

                val file = File(dataFolder, AuthenticationSettingsFilename)

                file.writeText(encryptedJson)

                return true
            }
        } catch (e: Exception) {
            log.error("Could not save AuthenticationSettings", e)
        }

        return false
    }

    protected open fun getAuthenticationSettingsFileKey(): Pair<SecretKey, Crypto> {
        val store = Store(applicationContext)

        val key = if (store.hasKey(AuthenticationSettingsFileKey)) store.getSymmetricKey(AuthenticationSettingsFileKey, AuthenticationSettingsFileKeyPassword)
                    else store.generateSymmetricKey(AuthenticationSettingsFileKey, AuthenticationSettingsFileKeyPassword)

        return Pair(key, Crypto(Options.TRANSFORMATION_SYMMETRIC))
    }


    open fun generateRandomPassword(): CharArray {
        return generateRandomPassword(30)
    }

    open fun generateRandomPassword(passwordLength: Int): CharArray {
        val dictionary = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789§±!@#$%^&*-_=+;:|/?.>,<"

        val password = CharArray(passwordLength)
        IntRange(0, passwordLength - 1).forEach { index ->
            password[index] = dictionary.random()
        }

        return password
    }


    open fun encodeToBase64(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.DEFAULT)
    }

    open fun decodeFromBase64(data: String): ByteArray {
        return Base64.decode(data, Base64.DEFAULT)
    }

}