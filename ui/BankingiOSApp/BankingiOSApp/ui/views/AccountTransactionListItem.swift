import SwiftUI
import BankingUiSwift


struct AccountTransactionListItem: View {
    
    private static var ValueDateFormat: DateFormatter = {
        let formatter = DateFormatter()
        
        formatter.dateStyle = .short
        
        return formatter
    }()
    
    
    let transaction: AccountTransaction
    
    
    @Inject private var presenter: BankingPresenterSwift
    
    
    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                Text(getTransactionLabel(transaction))
                
                Text(transaction.usage)
            }

            Spacer()

            VStack(alignment: .trailing) {
                Text(presenter.formatAmount(amount: transaction.amount))
                
                Spacer()
                
                Text(Self.ValueDateFormat.string(from: transaction.valueDate.date))
            }
        }
    }


    private func getTransactionLabel(_ transaction: AccountTransaction) -> String {
        if transaction.bookingText?.localizedCaseInsensitiveCompare("Bargeldauszahlung") == ComparisonResult.orderedSame {
            return transaction.bookingText ?? ""
        }

        if transaction.showOtherPartyName {
            return transaction.otherPartyName ?? ""
        }

        return transaction.bookingText ?? ""
    }

}

struct AccountTransactionListItem_Previews: PreviewProvider {
    static var previews: some View {
        AccountTransactionListItem(transaction: AccountTransaction(bankAccount: previewBanks[0].accounts[0], otherPartyName: "Marieke Musterfrau", unparsedUsage: "Vielen Dank für Ihre Mühen", amount: CommonBigDecimal(double: 1234.56), valueDate: CommonDate(year: 2020, month: .march, day_: 27), bookingText: "SEPA Überweisung"))
    }
}
