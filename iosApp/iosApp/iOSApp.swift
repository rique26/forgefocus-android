import SwiftUI
import Shared

@main
struct iOSApp: App {

    init() {
        SharedAppModuleKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}