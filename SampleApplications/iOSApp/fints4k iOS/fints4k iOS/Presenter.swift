import SwiftUI
import fints4k

class Presenter {
    
    private let fintsClient = FinTsClientDeprecated(callback: SimpleFinTsClientCallback(), webClient: UrlSessionWebClient())
    
    private let formatter = DateFormatter()
    
    
    func retrieveTransactions(_ bankCode: String, _ customerId: String, _ pin: String, _ finTs3ServerAddress: String, _ callback: @escaping (AddAccountResponse) -> Void) {
        self.fintsClient.addAccountAsync(parameter: AddAccountParameter(bankCode: bankCode, customerId: customerId, pin: pin, finTs3ServerAddress: finTs3ServerAddress), callback: callback)
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