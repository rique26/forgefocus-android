import SwiftUI
import Shared

@main
struct iOSApp: App {

    init() {
        AppModuleKt.initKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}