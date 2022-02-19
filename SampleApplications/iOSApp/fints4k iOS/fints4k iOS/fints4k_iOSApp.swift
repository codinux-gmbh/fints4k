
import SwiftUI
import fints4k

@main
struct fints4k_iOSApp: App {
    
    @StateObject var presenter = Presenter()
    
    @State private var isShowingEnterTanDialog = false
    
    // init with a default value
    @State private var tanChallenge = TanChallenge(messageToShowToUser: "", challenge: "", tanMethod: TanMethod(displayName: "", securityFunction: .pinTan900, type: .entertan, hhdVersion: .hhd13, maxTanInputLength: 6, allowedTanFormat: .numeric, nameOfTanMediumRequired: false, decoupledParameters: nil), tanMediaIdentifier: nil)
    
    
    var body: some Scene {
        WindowGroup {
            NavigationView {
                VStack {
                    NavigationLink(destination: EnterTanDialog(tanChallenge), isActive: $isShowingEnterTanDialog) { EmptyView() }
                    
                    ContentView()
                        .onAppear {
                            self.initApp()
                        }
                        .environmentObject(presenter)
                }
                .navigationBarHidden(true)
            }
        }
    }
    
    func initApp() {
        self.presenter.setEnterTanCallback { tanChallenge in
            self.tanChallenge = tanChallenge
            
            self.isShowingEnterTanDialog = true
        }
    }
    
}
