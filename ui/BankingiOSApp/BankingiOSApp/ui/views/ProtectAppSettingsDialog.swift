import SwiftUI


struct ProtectAppSettingsDialog: View {
    
    @Environment(\.presentationMode) var presentation
    
    
    private let authenticationService = AuthenticationService()
    
    private let supportedAuthenticationTypes: [AuthenticationType]
    
    @State private var isFaceIDSelected: Bool = false
    
    @State private var isTouchIDSelected: Bool = false
    
    @State private var isPasswordSelected: Bool = false
    
    @State private var isNoAppProtectionSelected: Bool = false
    
    @State private var selectedAuthenticationTypeIndex = 0
    
    private var selectedAuthenticationTypeIndexBinding: Binding<Int> {
        Binding<Int>(
            get: { self.selectedAuthenticationTypeIndex },
            set: {
                if (self.selectedAuthenticationTypeIndex != $0) { // only if authentication type really changed
                    self.selectedAuthenticationTypeIndex = $0
                    self.selectedAuthenticationTypeChanged(self.supportedAuthenticationTypes[$0])
                }
        })
    }
    
    
    @State private var newPassword: String = ""
    
    @State private var confirmedNewPassword: String = ""
    
    @State private var successfullyAuthenticatedWithBiometricAuthentication = false
    
    @State private var successfullyAuthenticatedWithPassword = false
    
    
    init() {
        let currentAuthenticationType = authenticationService.authenticationType
        
        var authenticationTypes = [AuthenticationType]()
        
        if authenticationService.deviceSupportsFaceID {
            authenticationTypes.append(.faceID)
        }
        if authenticationService.deviceSupportsTouchID {
            authenticationTypes.append(.touchID)
        }

        authenticationTypes.append(.password)
        
        if currentAuthenticationType != .none {
            authenticationTypes.append(.none)
        }
        
        self.supportedAuthenticationTypes = authenticationTypes
        
        
        if currentAuthenticationType == .faceID || (currentAuthenticationType != .password && authenticationService.deviceSupportsFaceID) {
            _isFaceIDSelected = State(initialValue: true)
            _selectedAuthenticationTypeIndex = State(initialValue: 0)
        }
        else if currentAuthenticationType == .touchID || (currentAuthenticationType != .password && authenticationService.deviceSupportsTouchID) {
            _isTouchIDSelected = State(initialValue: true)
            _selectedAuthenticationTypeIndex = State(initialValue: 0)
        }
        else {
            _isPasswordSelected = State(initialValue: true)
            _selectedAuthenticationTypeIndex = State(initialValue: supportedAuthenticationTypes.firstIndex(of: .password)!)
        }
    }
    

    var body: some View {
        Form {
            if supportedAuthenticationTypes.count > 1 {
                Section {
                    Picker("", selection: selectedAuthenticationTypeIndexBinding) {
                        ForEach(0..<supportedAuthenticationTypes.count) { index in
                            Text(self.supportedAuthenticationTypes[index].rawValue.firstLetterUppercased.localize())
                                .tag(index)
                        }
                    }
                    .pickerStyle(SegmentedPickerStyle())
                    .alignHorizontally(.center)
                }
            }
            
            if isFaceIDSelected {
                Section {
                    FaceIDButton(self.doBiometricAuthentication)
                        .alignHorizontally(.center)
                }
            }
            
            if isTouchIDSelected {
                Section {
                    TouchIDButton(self.doBiometricAuthentication)
                        .alignHorizontally(.center)
                }
            }
            
            if isPasswordSelected {
                Section {
                    LabelledUIKitTextField(label: "Password", text: $newPassword.didSet(self.enteredPasswordChanged), placeholder: "Enter new password", isPasswordField: true,
                                           focusOnStart: true, focusNextTextFieldOnReturnKeyPress: true, actionOnReturnKeyPress: handleReturnKeyPress)
                    
                    LabelledUIKitTextField(label: "Confirm password", text: $confirmedNewPassword.didSet(self.enteredPasswordChanged), placeholder: "Confirm new password",
                                           isPasswordField: true, actionOnReturnKeyPress: handleReturnKeyPress)
                }
            }
            
            if isNoAppProtectionSelected {
                Section {
                    Text("Do you really want to remove app protection?")
                        .multilineTextAlignment(.center)
                }
            }
            
            Section {
                HStack {
                    Spacer()
                    
                    Button(isNoAppProtectionSelected ? "Remove app protection" : "OK") { self.setAuthenticationType() }
                        .alignHorizontally(.center)
                        .foregroundColor(isNoAppProtectionSelected ? Color.destructive : nil)
                        .disabled( !self.authenticatedWithNewAuthenticationType)
                    
                    Spacer()
                }
                .frame(maxWidth: .infinity, minHeight: 40)
            }
        }
        .fixKeyboardCoversLowerPart()
        .showNavigationBarTitle("Protect App Settings Dialog title")
    }
    
    
    private func selectedAuthenticationTypeChanged(_ type: AuthenticationType) {
        isFaceIDSelected = false
        isTouchIDSelected = false
        isPasswordSelected = false
        isNoAppProtectionSelected = false
    
        if type == .faceID {
            isFaceIDSelected = true
        }
        else if type == .touchID {
            isTouchIDSelected = true
        }
        else if type == .password {
            isPasswordSelected = true
        }
        else if type == .none {
            isNoAppProtectionSelected = true
        }
        
        if isPasswordSelected == false {
            UIApplication.hideKeyboard()
        }
    }
    
    private func doBiometricAuthentication() {
        authenticationService.loginWithBiometricAuthentication { success, errorMessage in
            self.successfullyAuthenticatedWithBiometricAuthentication = success
        }
    }
    
    private func enteredPasswordChanged(_ oldValue: String, _ newValue: String) {
        successfullyAuthenticatedWithPassword = newPassword.isNotBlank && newPassword == confirmedNewPassword
    }
    
    private var authenticatedWithNewAuthenticationType: Bool {
        ((isFaceIDSelected || isTouchIDSelected) && successfullyAuthenticatedWithBiometricAuthentication) ||
            (isPasswordSelected && successfullyAuthenticatedWithPassword) ||
            isNoAppProtectionSelected
    }

    func handleReturnKeyPress() -> Bool {
        if authenticatedWithNewAuthenticationType {
            self.setAuthenticationType()
            
            return true
        }
        
        return false
    }
    
    private func setAuthenticationType() {
        if isFaceIDSelected {
            authenticationService.setAuthenticationType(.faceID)
        }
        else if isTouchIDSelected {
            authenticationService.setAuthenticationType(.touchID)
        }
        else if isPasswordSelected {
            authenticationService.setAuthenticationTypeToPassword(newPassword)
        }
        else if isNoAppProtectionSelected {
            authenticationService.setAuthenticationType(.none)
        }
        
        presentation.wrappedValue.dismiss()
    }

}


struct ProtectAppSettingsDIalog_Previews: PreviewProvider {

    static var previews: some View {
        ProtectAppSettingsDialog()
    }

}
