import SwiftUI
import Shared

@main
struct iOSApp: App {

    // Kotlin/Native antepone "do" a las funciones que empiezan con "init".
    init() {
        KoinIosKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
