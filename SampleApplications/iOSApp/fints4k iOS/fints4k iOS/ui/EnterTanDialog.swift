import SwiftUI
import fints4k


struct EnterTanDialog: View {
    
    @Environment(\.presentationMode) var presentation
    
    
    private var tanChallenge: TanChallenge
    
    private let imageTanChallenge: ImageTanChallenge?
    
    private let messageToShowToUser: String
    
    @State private var enteredTan = ""

    private var senteredTanBinding: Binding<String> {
        Binding<String>(
            get: { self.enteredTan },
            set: {
                self.enteredTan = $0
        })
    }
    
    
    @EnvironmentObject private var presenter: Presenter
    
    
    init(_ tanChallenge: TanChallenge) {
        self.tanChallenge = tanChallenge
        
        self.imageTanChallenge = tanChallenge as? ImageTanChallenge
        
        self.messageToShowToUser = tanChallenge.messageToShowToUser//.htmlToString // parse in init() calling this method in body { } crashes application
    }
    
    
    var body: some View {
        Form {
            imageTanChallenge.map { imageTanChallenge in
                Image(uiImage: UIImage(data: imageTanChallenge.image.imageBytesAsNSData())!)
            }
            
            VStack {
                HStack {
                    Text("TAN hint from your bank:")
                    Spacer()
                }
                
                HStack {
                    Text(messageToShowToUser)
                        .multilineTextAlignment(.leading)
                        .lineLimit(5) // hm, we doesn't it show more then three lines?
                    
                    Spacer()
                }
                .padding(.top, 6)
            }
            .padding(.vertical, 2)
            
            Section {
                HStack(alignment: .center) {
                    Text("Enter TAN:")

                    Spacer()

                    TextField("Enter the TAN here", text: self.$enteredTan)
                        .keyboardType(tanChallenge.tanMethod.isNumericTan ? .numberPad : .default)
                        .autocapitalization(.none)
                }
            }
            
            Section {
                HStack {
                    Spacer()
                    Button(action: { self.enteringTanDone() },
                           label: { Text("OK") })
                        .disabled( !self.isRequiredDataEntered())
                    Spacer()
                }
            }
        }
        .navigationTitle("Enter TAN")
        .navigationBarTitleDisplayMode(.inline)
    }
    
    
    private func isRequiredDataEntered() -> Bool {
        return self.enteredTan.isEmpty == false
    }
    
    
    private func enteringTanDone() {
        if self.isRequiredDataEntered() {
            self.tanChallenge.userEnteredTan(enteredTan: self.enteredTan)
        } else {
            self.tanChallenge.userDidNotEnterTan()
        }
        
        // TODO: if a TAN has been entered, check result if user has made a mistake and has to re-enter TAN
        dismissDialog()
    }
    
    private func dismissDialog() {
        presentation.wrappedValue.dismiss()
    }

}


//struct EnterTanDialog_Previews: PreviewProvider {
//
//    static var previews: some View {
//        EnterTanDialog()
//    }
//
//}
