import SwiftUI
import fints4k

class Presenter : ObservableObject {
    
//    var enterTanCallback: ((TanChallenge) -> Void)? = nil
    
    // Swift, you're so stupid! It seems to be impossible to initialize SimpleFinTsClientCallback here so that enterTanCallback gets called if set
    private var fintsClient = iOSFinTsClient(callback: SimpleFinTsClientCallback(), webClient: UrlSessionWebClient())
    
    private let formatter = DateFormatter()
    
    
    func setEnterTanCallback(enterTanCallback: @escaping (TanChallenge) -> Void) {
        self.fintsClient.callback = SimpleFinTsClientCallback( enterTan: { tanChallenge in
            enterTanCallback(tanChallenge)
        })
    }
    
    
    func getAccountData(_ bankCode: String, _ customerId: String, _ pin: String, _ finTs3ServerAddress: String, _ callback: @escaping (GetAccountDataResponse) -> Void) {
        self.fintsClient.getAccountDataAsync(parameter: GetAccountDataParameter(bankCode: bankCode, customerId: customerId, pin: pin, finTs3ServerAddress: finTs3ServerAddress), callback: callback)
    }
    
    
    func formatDate(_ date: Kotlinx_datetimeLocalDate) -> String {
        formatter.dateStyle = .short
        
//        return self.formatter.string(from: date.toNSDate())
        
        return date.description()
    }
    
    func formatAmount(_ amount: Money) -> String {
        return amount.description()
    }
    
}
