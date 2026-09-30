import SwiftUI
import shared

struct ContentView: View {
    let platform = Platform_iosKt.getPlatform()
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "location.fill")
                .imageScale(.large)
                .font(.system(size: 48))
                .foregroundColor(.blue)
            
            Text("📍 PingPin")
                .font(.largeTitle)
                .bold()
            
            Text("Privacy-First Hybrid Work & Attendance Assistant")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Divider()
                .padding(.vertical)
            
            VStack(alignment: .leading, spacing: 10) {
                Label("Running on: \(platform.name)", systemImage: "iphone")
                    .font(.footnote)
                    .foregroundColor(.gray)
            }
            .padding()
            .background(Color(.systemGray6))
            .cornerRadius(12)
        }
        .padding()
    }
}

#Preview {
    ContentView()
}
