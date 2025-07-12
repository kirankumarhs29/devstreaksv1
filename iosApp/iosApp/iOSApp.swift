import SwiftUI
import FirebaseCore

@main
struct DevstreaksApp: App {
	init() {
		FirebaseApp.configure() // ✅ still required
	}
	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}