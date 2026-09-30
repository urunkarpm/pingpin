import SwiftUI
import shared

struct SettingsView: View {
    @State private var officeSsid: String = "Office_5G_Guest"
    @State private var shiftStartTime: Date = Date()
    @State private var selectedState: String = "Maharashtra (MH)"
    @State private var autoCheckInEnabled: Bool = true
    @State private var autoPunchHrPortal: Bool = true
    
    let indianStates = [
        "All States (National)",
        "Maharashtra (MH)",
        "Karnataka (KA)",
        "Delhi (DL)",
        "Tamil Nadu (TN)",
        "Telangana (TG)",
        "Gujarat (GJ)",
        "West Bengal (WB)"
    ]

    var body: some View {
        NavigationStack {
            Form {
                Section(header: Text("OFFICE WI-FI & ATTENDANCE")) {
                    HStack {
                        Label("Office Network SSID", systemImage: "wifi")
                        Spacer()
                        TextField("SSID", text: $officeSsid)
                            .multilineTextAlignment(.trailing)
                            .foregroundColor(.blue)
                    }
                    
                    Toggle(isOn: $autoCheckInEnabled) {
                        Label("Auto Check-In on Connect", systemImage: "bolt.badge.clock")
                    }
                }
                
                Section(header: Text("WORK SHIFT TIMING")) {
                    DatePicker(selection: $shiftStartTime, displayedComponents: .hourAndMinute) {
                        Label("Configured Shift Start", systemImage: "clock")
                    }
                }

                Section(header: Text("HR PORTAL AUTO-PUNCH")) {
                    Toggle(isOn: $autoPunchHrPortal) {
                        Label("Auto Check-In HR Web Portal", systemImage: "safari")
                    }
                }
                
                Section(header: Text("INDIAN HOLIDAY DIRECTORY")) {
                    Picker(selection: $selectedState) {
                        ForEach(indianStates, id: \.self) { state in
                            Text(state).tag(state)
                        }
                    } label: {
                        Label("State / Region Filter", systemImage: "mappin.and.ellipse")
                    }
                }
                
                Section(header: Text("ABOUT PINGPIN")) {
                    HStack {
                        Text("Version")
                        Spacer()
                        Text("3.0.0 (KMP iOS Native)")
                            .foregroundColor(.secondary)
                    }
                    HStack {
                        Text("Privacy Guarantee")
                        Spacer()
                        Text("100% Local On-Device")
                            .bold()
                            .foregroundColor(.green)
                    }
                }
            }
            .navigationTitle("Settings")
        }
    }
}

#Preview {
    SettingsView()
}
