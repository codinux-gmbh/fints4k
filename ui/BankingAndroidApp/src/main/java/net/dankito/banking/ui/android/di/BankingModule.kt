package net.dankito.banking.ui.android.di

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import dagger.Module
import dagger.Provides
import net.dankito.utils.multiplatform.File
import net.dankito.banking.ui.android.RouterAndroid
import net.dankito.banking.ui.android.util.CurrentActivityTracker
import net.dankito.banking.fints4kBankingClientCreator
import net.dankito.banking.persistence.IBankingPersistence
import net.dankito.banking.search.ITransactionPartySearcher
import net.dankito.banking.ui.IBankingClientCreator
import net.dankito.banking.ui.IRouter
import net.dankito.banking.ui.presenter.BankingPresenter
import net.dankito.banking.bankfinder.IBankFinder
import net.dankito.banking.bankfinder.InMemoryBankFinder
import net.dankito.banking.bankfinder.LuceneBankFinder
import net.dankito.banking.persistence.RoomBankingPersistence
import net.dankito.banking.persistence.model.RoomModelCreator
import net.dankito.banking.ui.android.authentication.AuthenticationService
import net.dankito.banking.ui.android.authentication.BiometricAuthenticationService
import net.dankito.banking.ui.android.authentication.IBiometricAuthenticationService
import net.dankito.banking.ui.model.mapper.IModelCreator
import net.dankito.banking.ui.util.CurrencyInfoProvider
import net.dankito.utils.multiplatform.toFile
import net.dankito.banking.util.*
import net.dankito.banking.util.extraction.*
import net.dankito.text.extraction.TextExtractorRegistry
import net.dankito.text.extraction.pdf.PdfBoxAndroidPdfTextExtractor
import net.dankito.text.extraction.pdf.iText2PdfTextExtractor
import net.dankito.utils.ThreadPool
import net.dankito.utils.android.permissions.IPermissionsService
import net.dankito.utils.android.permissions.PermissionsService
import net.dankito.utils.web.client.IWebClient
import net.dankito.utils.web.client.OkHttpWebClient
import javax.inject.Named
import javax.inject.Singleton


@Module
class BankingModule(private val applicationContext: Context) {

    companion object {

        const val DataFolderKey = "data.folder"

        const val DatabaseFolderKey = "database.folder"

        const val IndexFolderKey = "index.folder"

        // TODO: implement activity listener to always get latest activity
        lateinit var mainActivity: AppCompatActivity
    }


    @Provides
    @Singleton
    fun provideApplicationContext() : Context {
        return applicationContext
    }

    @Provides
    @Singleton
    @Named(DataFolderKey)
    fun provideDataFolder(applicationContext: Context) : File {
        return ensureFolderExists(applicationContext.filesDir.toFile(), "data")
    }

    @Provides
    @Singleton
    @Named(DatabaseFolderKey)
    fun provideDatabaseFolder(@Named(DataFolderKey) dataFolder: File) : File {
        return ensureFolderExists(dataFolder, "db")
    }

    @Provides
    @Singleton
    @Named(IndexFolderKey)
    fun provideIndexFolder(@Named(DataFolderKey) dataFolder: File) : File {
        return ensureFolderExists(dataFolder, "index")
    }

    private fun ensureFolderExists(parentFolder: File, folderName: String): File {
        val folder = File(parentFolder, folderName)

        folder.mkdirs()

        return folder
    }


    @Provides
    @Singleton
    fun providePermissionsService() : IPermissionsService {
        return PermissionsService(mainActivity)
    }


    @Provides
    @Singleton
    fun provideAuthenticationService(biometricAuthenticationService: IBiometricAuthenticationService, persistence: IBankingPersistence,
                                     @Named(DataFolderKey) dataFolder: File, serializer: ISerializer) : AuthenticationService {
        return AuthenticationService(applicationContext, biometricAuthenticationService, persistence, dataFolder, serializer)
    }

    @Provides
    @Singleton
    fun provideBiometricAuthenticationService(context: Context, currentActivityTracker: CurrentActivityTracker) : IBiometricAuthenticationService {
        return BiometricAuthenticationService(context, currentActivityTracker)
    }


    @Provides
    @Singleton
    fun provideBankingPresenter(bankingClientCreator: IBankingClientCreator, bankFinder: IBankFinder,
                                @Named(DataFolderKey) dataFolder: File,
                                persister: IBankingPersistence, transactionPartySearcher: ITransactionPartySearcher, bankIconFinder: IBankIconFinder,
                                textExtractorRegistry: ITextExtractorRegistry, router: IRouter, invoiceDataExtractor: IInvoiceDataExtractor,
                                modelCreator: IModelCreator, asyncRunner: IAsyncRunner) : BankingPresenter {
        return BankingPresenter(bankingClientCreator, bankFinder, dataFolder, persister, router, modelCreator,
            transactionPartySearcher, bankIconFinder, textExtractorRegistry, invoiceDataExtractor, CurrencyInfoProvider(), asyncRunner)
    }

    @Provides
    @Singleton
    fun provideBankFinder(@Named(IndexFolderKey) indexFolder: File) : IBankFinder {
        //return LuceneBankFinder(indexFolder) // TODO: undo
        return InMemoryBankFinder()
    }

    @Provides
    @Singleton
    fun provideBankIconFinder() : IBankIconFinder {
        return BankIconFinder()
    }

    @Provides
    @Singleton
    fun provideBankingClientCreator(modelCreator: IModelCreator, serializer: ISerializer) : IBankingClientCreator {
        return fints4kBankingClientCreator(modelCreator, serializer)
    }

    @Provides
    @Singleton
    fun provideBankingPersistence() : IBankingPersistence {
        return RoomBankingPersistence(applicationContext)
    }

    @Provides
    @Singleton
    fun provideTransactionPartySearcher(bankingPersistence: IBankingPersistence) : ITransactionPartySearcher {
        return bankingPersistence as RoomBankingPersistence
    }

    @Provides
    @Singleton
    fun provideCurrentActivityTracker() : CurrentActivityTracker {
        return CurrentActivityTracker()
    }

    @Provides
    @Singleton
    fun provideRouter(currentActivityTracker: CurrentActivityTracker) : IRouter {
        return RouterAndroid(currentActivityTracker)
    }


    @Provides
    @Singleton
    fun provideTextExtractorRegistry(applicationContext: Context) : ITextExtractorRegistry {
        // TODO: add PdfTypeDetector
        return JavaTextExtractorRegistry(TextExtractorRegistry(listOf(
            iText2PdfTextExtractor(), PdfBoxAndroidPdfTextExtractor(applicationContext)
        )))
    }

    @Provides
    @Singleton
    fun provideInvoiceDataExtractor() : IInvoiceDataExtractor {
        return JavaInvoiceDataExtractor()
    }


    @Provides
    @Singleton
    fun provideWebClient() : IWebClient {
        return OkHttpWebClient()
    }

    @Provides
    @Singleton
    fun provideSerializer() : ISerializer {
        return JacksonJsonSerializer()
    }

    @Provides
    @Singleton
    fun provideModelCreator() : IModelCreator {
        return RoomModelCreator()
    }

    @Provides
    @Singleton
    fun provideAsyncRunner() : IAsyncRunner {
        return ThreadPoolAsyncRunner(ThreadPool())
    }

}