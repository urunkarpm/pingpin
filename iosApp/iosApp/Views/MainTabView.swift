import SwiftUI

struct MainTabView: View {
    var body: some View {
        TabView {
            HomeView()
                .tabItem {
                    Label("Home", systemImage: "house.fill")
                }
            
            InsightsView()
                .tabItem {
                    Label("Analytics", systemImage: "chart.bar.doc.horizontal.fill")
                }
            
            HRPortalView()
                .tabItem {
                    Label("HR Portal", systemImage: "safari.fill")
                }
            
            HolidaysView()
                .tabItem {
                    Label("Holidays", systemImage: "calendar.badge.clock")
                }
            
            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
        }
    }
}

#Preview {
    MainTabView()
}
