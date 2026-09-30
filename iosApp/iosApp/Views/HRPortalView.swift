import SwiftUI
import WebKit

struct WebViewRepresentable: UIViewRepresentable {
    let url: URL
    
    func makeUIView(context: Context) -> WKWebView {
        let prefs = WKWebpagePreferences()
        prefs.allowsContentJavaScript = true
        let config = WKWebViewConfiguration()
        config.defaultWebpagePreferences = prefs
        return WKWebView(frame: .zero, configuration: config)
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {
        let request = URLRequest(url: url)
        uiView.load(request)
    }
}

struct HRPortalView: View {
    @State private var portalUrlString: String = "https://hr.company.com"
    @State private var isAutoPunchActive: Bool = true
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Address & Action Bar
                HStack {
                    Image(systemName: "lock.fill")
                        .font(.caption)
                        .foregroundColor(.green)
                    Text(portalUrlString)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(1)
                    Spacer()
                    
                    Button(action: {
                        // Triggers Auto Punch Script Injection
                    }) {
                        Label("Auto Punch", systemImage: "bolt.fill")
                            .font(.caption2)
                            .bold()
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(12)
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                .background(Color(.systemGray6))

                // WKWebView Container
                if let url = URL(string: portalUrlString) {
                    WebViewRepresentable(url: url)
                } else {
                    ContentUnavailableView(
                        "Invalid HR Portal URL",
                        systemImage: "safari",
                        description: Text("Please configure your company HR Portal URL in Settings.")
                    )
                }
            }
            .navigationTitle("HR Web Portal")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

#Preview {
    HRPortalView()
}
