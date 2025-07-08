import SwiftUI
import UIKit
import composeApp

struct ContentView: View {
	func makeUIViewController(context: Context) -> UIViewController {
            MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView_Previews: PreviewProvider {
	static var previews: some View {
		ContentView()
	}
}