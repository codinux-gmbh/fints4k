import SwiftUI
import BankingUiSwift


struct AccountsTab: View {
    
    @Inject private var presenter: BankingPresenterSwift
    
    @ObservedObject var data: AppData


    var body: some View {
        VStack {
            if data.banks.isEmpty {
                Spacer()

                AddAccountButtonView()

                Spacer()
            }
            else {
                Form {
                    AllBanksListItem(banks: data.banks)

                    ForEach(data.banks) { bank in
                        BankListItem(bank: bank)
                    }

                    Section {
                        AddAccountButtonView()
                    }
                    .systemGroupedBackground()
                    .listRowInsets(EdgeInsets())

                }
            }
        }
        .systemGroupedBackground()
    }

}


struct AccountsTab_Previews: PreviewProvider {
    
    static var previews: some View {
        let data = AppData()
        data.banks = previewBanks
        
        return AccountsTab(data: data)
    }
    
}
