//package net.dankito.banking.fints
//
//import ch.tutteli.atrium.api.fluent.en_GB.*
//import ch.tutteli.atrium.api.verbs.expect
//import jdk.nashorn.internal.ir.annotations.Ignore
//import net.dankito.banking.bankfinder.InMemoryBankFinder
//import net.codinux.banking.fints.callback.FinTsClientCallback
//import net.codinux.banking.fints.extensions.isTrue
//import net.codinux.banking.fints.extensions.isFalse
//import net.codinux.banking.fints.messages.datenelemente.implementierte.Dialogsprache
//import net.codinux.banking.fints.messages.datenelemente.implementierte.KundensystemStatus
//import net.codinux.banking.fints.messages.datenelemente.implementierte.KundensystemStatusWerte
//import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanEinsatzOption
//import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanGeneratorTanMedium
//import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMedienArtVersion
//import net.codinux.banking.fints.messages.datenelemente.implementierte.tan.TanMediumKlasse
//import net.codinux.banking.fints.messages.segmente.id.CustomerSegmentId
//import net.codinux.banking.fints.model.*
//import net.codinux.banking.fints.response.client.*
//import net.dankito.utils.multiplatform.Date
//import net.dankito.utils.multiplatform.DateFormatter
//import net.dankito.utils.multiplatform.UUID
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.fail
//import java.util.concurrent.CountDownLatch
//import java.util.concurrent.TimeUnit
//import java.util.concurrent.atomic.AtomicReference
//
//
//@Ignore // not an automatic test, supply your settings below
//open class FinTsClientTestBase {
//
//    companion object {
//
//        // TODO: add your settings here:
//        val BankCode = "<your bank code (BLZ) here>"
//
//        val CustomerId = "<your customer id (Online-Banking Login Name) here>"
//
//        val Password = "<your PIN (Online-Banking Passwort) here>"
//
//
//        val DateTimeFormatForUniqueBankTransferReference = DateFormatter("yyyy-MM-dd'T'HH:mm:ss.SSS")
//    }
//
//
//    private var didAskUserForTanMethod = false
//
//    private var didAskUserToEnterTan = false
//
//
//    private val callback = object : FinTsClientCallback {
//
//        override fun askUserForTanMethod(supportedTanMethods: List<TanMethod>, suggestedTanMethod: TanMethod?, callback: (TanMethod?) -> Unit) {
//            didAskUserForTanMethod = true
//            callback(suggestedTanMethod) // simply return suggestedTanMethod as in most cases it's the best fitting one
//        }
//
//        override fun enterTan(bank: BankData, tanChallenge: TanChallenge, callback: (EnterTanResult) -> Unit) {
//            didAskUserToEnterTan = true
//
//            callback(EnterTanResult.userDidNotEnterTan())
//        }
//
//        override fun enterTanGeneratorAtc(bank: BankData, tanMedium: TanGeneratorTanMedium, callback: (EnterTanGeneratorAtcResult) -> Unit) {
//            fail("Bank asks you to synchronize your TAN generator for card ${tanMedium.cardNumber} " +
//                    "(card sequence number ${tanMedium.cardSequenceNumber}). Please do this via your online banking portal or Banking UI.")
//        }
//
//    }
//
//
//    private val underTest = FinTsClient(callback)
//
//
//    private val BankDataAnonymous = BankData.anonymous("10070000", "https://fints.deutsche-bank.de/", "DEUTDEBBXXX")
//
//    private val bankInfo = InMemoryBankFinder().findBankByBankCode(BankCode).first()
//    private val Bank = BankData(bankInfo.bankCode, CustomerId, Password, bankInfo.pinTanAddress ?: "", bankInfo.bic, bankInfo.name)
//
//
//
//    @Test
//    fun getAnonymousBankInfo() {
//
//        // when
//        underTest.getAnonymousBankInfo(BankDataAnonymous) { result ->
//
//            // then
//            assertTrue(result.successful)
//            expect(BankDataAnonymous.supportedHbciVersions).isNotEmpty()
//            expect(BankDataAnonymous.tanMethodsSupportedByBank).isNotEmpty()
//            expect(BankDataAnonymous.supportedJobs).isNotEmpty()
//            expect(BankDataAnonymous.supportedLanguages).isNotEmpty()
//            expect(BankDataAnonymous.bankName).isNotEmpty()
//        }
//    }
//
//
//    @Test
//    fun addAccount() {
//
//        // given
//        val response = AtomicReference<AddAccountResponse>()
//        val countDownLatch = CountDownLatch(1)
//
//
//        // when
//        underTest.addAccountAsync(Bank.toAddAccountParameter()) {
//            response.set(it)
//            countDownLatch.countDown()
//        }
//
//
//        // then
//        countDownLatch.await(30, TimeUnit.SECONDS)
//        val result = response.get()
//
//        assertTrue(result.successful)
//
//        assertFalse(didAskUserForTanMethod)
//
//        expect(Bank.bankName).isNotEmpty()
//        expect(Bank.supportedJobs).isNotEmpty() // supported jobs are now known
//        expect(Bank.tanMethodsSupportedByBank).isNotEmpty() // supported tan methods are now known
//        expect(Bank.supportedHbciVersions).isNotEmpty() // supported HBIC versions are now known
//        expect(Bank.supportedLanguages).isNotEmpty() // supported languages are now known
//
//        expect(Bank.customerName).isNotEmpty()
//        expect(Bank.tanMethodsAvailableForUser).isNotEmpty()
//        expect(Bank.selectedLanguage).notToBe(Dialogsprache.Default) // language is set now
//        expect(Bank.customerSystemId).notToBe(KundensystemStatus.SynchronizingCustomerSystemId.code) // customer system id is now set
//        assertEquals(KundensystemStatusWerte.Benoetigt, Bank.customerSystemStatus) // customerSystemStatus is set now
//        expect(Bank.accounts).isNotEmpty() // accounts are now known
//        expect(Bank.accounts.first().allowedJobs).isNotEmpty() // allowed jobs are now known
//    }
//
//
//    @ExperimentalWithOptions
//    @Test
//    fun getTransactions() {
//
//        // given
//        val response = AtomicReference<GetAccountTransactionsResponse>()
//        val countDownLatch = CountDownLatch(1)
//
//        underTest.addAccountAsync(Bank.toAddAccountParameter(false)) { // retrieve basic data, e.g. accounts
//            val account = Bank.accounts.firstOrNull { it.supportsFeature(AccountFeature.RetrieveAccountTransactions) }
//            expect(account).withRepresentation("We need at least one account that supports retrieving account transactions (${CustomerSegmentId.AccountTransactionsMt940.id})").notToBeNull()
//
//            // when
//
//            // some banks support retrieving account transactions of last 90 days without TAN
//            underTest.tryGetAccountTransactionsOfLast90DaysWithoutTan(Bank, account!!) {
//                response.set(it)
//                countDownLatch.countDown()
//            }
//        }
//
//
//        // then
//        countDownLatch.await(30, TimeUnit.SECONDS)
//        val result = response.get()
//
//        assertThat(result.successful).isTrue()
//        assertThat(result.retrievedData).isNotNull()
//    }
//
//
//    @Test
//    fun getTanMediaList() {
//
//        // this test is only senseful for accounts using chipTAN / TAN generator as TAN method
//
//        // given
//        val response = AtomicReference<GetTanMediaListResponse>()
//        val countDownLatch = CountDownLatch(1)
//        val anonymousBankInfoCountDownLatch = CountDownLatch(1)
//
//
//        underTest.getAnonymousBankInfo(Bank) {
//            anonymousBankInfoCountDownLatch.countDown()
//        }
//        anonymousBankInfoCountDownLatch.await(30, TimeUnit.SECONDS)
//
//
//        val supportsRetrievingTanMedia = Bank.supportedJobs.firstOrNull { it.jobName == "HKTAB" } != null
//
//        if (supportsRetrievingTanMedia == false) { // accounts with appTAN, pushTAN, smsTAN, ... would fail here -> simply return
//            println("Bank ${Bank.bankName} does not support retrieving TAN media. Therefore cannot execute test getTanMediaList()")
//            return
//        }
//
//        expect(Bank.tanMedia).isEmpty()
//
//
//        // when
//        underTest.getTanMediaList(Bank, TanMedienArtVersion.Alle, TanMediumKlasse.AlleMedien) { result ->
//            response.set(result)
//            countDownLatch.countDown()
//        }
//
//        // then
//        countDownLatch.await(30, TimeUnit.SECONDS)
//        val result = response.get()
//
//        assertTrue(result.successful)
//
//        expect(result.tanMediaList).notToBeNull()
//        expect(result.tanMediaList!!.usageOption).toBe(TanEinsatzOption.KundeKannGenauEinMediumZuEinerZeitNutzen) // TODO: may adjust to your value
//        expect(result.tanMediaList!!.tanMedia).isNotEmpty()
//
//        expect(Bank.tanMedia).isNotEmpty()
//    }
//
//    @Ignore // only works with banks that don't support HKTAB version 5
//    @Test
//    fun getTanMediaList_UnsupportedTanMediumClass() {
//
//        // when
//        expect {
//            underTest.getTanMediaList(Bank, TanMedienArtVersion.Alle, TanMediumKlasse.BilateralVereinbart) { }
//        }.toThrow<UnsupportedOperationException>()
//
//
//        // then
//        // exception gets thrown
//    }
//
//
//    @ExperimentalWithOptions
//    @Test
//    fun testBankTransfer() {
//
//        // given
//        val response = AtomicReference<FinTsClientResponse>()
//        val countDownLatch = CountDownLatch(1)
//
//        underTest.addAccountAsync(Bank.toAddAccountParameter(false)) { // retrieve basic data, e.g. accounts
//            // we need at least one account that supports cash transfer
//            val account = Bank.accounts.firstOrNull { it.supportsFeature(AccountFeature.TransferMoney) }
//            expect(account).withRepresentation("We need at least one account that supports cash transfer (${CustomerSegmentId.SepaBankTransfer.id})").notToBeNull()
//
//            // IBAN should be set
//            expect(account?.iban).withRepresentation("Account IBAN must be set").notToBeNull()
//
//            // transfer 1 cent to yourself. Transferring money to oneself also doesn't require to enter a TAN according to PSD2
//            val BankTransferData = BankTransferData(Bank.customerName, account?.iban!!, Bank.bic, Money(Amount("0,01"), "EUR"),
//                "${DateTimeFormatForUniqueBankTransferReference.format(Date())} Test transaction ${UUID.random()}")
//
//
//            // when
//            underTest.doBankTransferAsync(BankTransferData, Bank, account) { result ->
//                response.set(result)
//                countDownLatch.countDown()
//            }
//
//        }
//
//
//        // then
//        countDownLatch.await(30, TimeUnit.SECONDS)
//        val result = response.get()
//
//        assertTrue(result.successful)
//
//    }
//
//}