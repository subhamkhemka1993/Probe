import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        ProbeBootstrapKt.installProbeSample()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
