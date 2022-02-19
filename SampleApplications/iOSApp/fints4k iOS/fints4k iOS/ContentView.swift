
import SwiftUI
import fints4k

struct ContentView: View {
    
    @State var transactions: [AccountTransaction] = []
    
    @EnvironmentObject private var presenter: Presenter
    
    
    var body: some View {
        VStack {
            Button("Fetch transactions") { self.retrieveTransactions() }
                .padding(.top, 6)
            
            List(self.transactions, id: \.self) { transaction in
                HStack {
                    VStack(alignment: .leading) {
                        Text(transaction.otherPartyName ?? transaction.bookingText ?? "")
                            .font(.headline)
                            .lineLimit(1)
                            .frame(height: 20)
                        
                        Text(transaction.reference)
                            .styleAsDetail()
                            .padding(.top, 4)
                            .lineLimit(2)
                            .frame(height: 46, alignment: .center)
                    }

                    Spacer()

                    VStack(alignment: .trailing) {

                        Text(presenter.formatAmount(transaction.amount))
                            .styleAsDetail()
                        
                        Spacer()
                        
                        Text(presenter.formatDate(transaction.valueDate))
                            .styleAsDetail()
                        
                        Spacer()
                    }
                }
            }
        }
    }
    
    
    private func retrieveTransactions() {
        // TODO: set your credentials here
        self.presenter.retrieveTransactions("", "", "", "", self.handleRetrieveTransactionsResult)
    }
    
    private func handleRetrieveTransactionsResult(_ result: AddAccountResponse) {
        NSLog("Retrieved response: \(result.retrievedTransactionsResponses)")
        
        if (result.successful) {
            var allTransactions: [AccountTransaction] = []
            
            for accountResponse in result.retrievedTransactionsResponses {
                if let transactions = accountResponse.retrievedData?.bookedTransactions as? Set<AccountTransaction> { // it's a Set
                    allTransactions.append(contentsOf: transactions)
                }
                if let transactions = accountResponse.retrievedData?.bookedTransactions as? [AccountTransaction] {
                    allTransactions.append(contentsOf: transactions)
                }
            }
            
            self.transactions = allTransactions
        }
    }
    
}


struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
