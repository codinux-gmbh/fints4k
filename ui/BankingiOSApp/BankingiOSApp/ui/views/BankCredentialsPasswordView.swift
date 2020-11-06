import SwiftUI


struct BankCredentialsPasswordView: View {
    
    @Binding private var password: String
    
    private var handleReturnKeyPress: (() -> Bool)? = nil
    
    
    init(_ password: Binding<String>, _ handleReturnKeyPress: (() -> Bool)? = nil) {
        self._password = password
        self.handleReturnKeyPress = handleReturnKeyPress
    }
    

    @ViewBuilder
    var body: some View {
        LabelledUIKitTextField(label: "Online banking login password", text: $password, placeholder: "Enter Online banking login password",
                               autocapitalizationType: .none, isPasswordField: true, actionOnReturnKeyPress: handleReturnKeyPress)
    }

}


struct BankCredentialsPasswordView_Previews: PreviewProvider {

    static var previews: some View {
        BankCredentialsPasswordView(.constant(""))
    }

}
