import SwiftUI
import shared

struct HomeView: View {
    @State private var isOfficeWifiConnected: Bool = false
    @State private var currentWifiSsid: String = "Office_5G_Guest"
    @State private var attendanceStatus: String = "Present on Time"
    
    // Sample state data consuming shared KMP models
    let sampleWeather = WeatherState(
        locationName: "Office Campus",
        currentTempC: 28,
        feelsLikeC: 30,
        tempHighC: 32,
        tempLowC: 24,
        condition: WeatherCondition.partlyCloudy,
        rainChancePercent: 20,
        hourlyForecast: [],
        insight: TravelInsight(
            headline: "Good Commute Conditions",
            detail: "Clear weather expected during peak commute hours.",
            umbrellaNeeded: false,
            recommendedTransport: "Metro / Car",
            travelSafetyScore: "9/10"
        ),
        lastUpdatedMillis: 0,
        hasValidData: true,
        isInitialLoading: false,
        isRefreshing: false,
        isError: false,
        errorMessage: nil,
        isStale: false,
        staleAgeMinutes: 0,
        locationStatus: LocationStatus.resolved,
        forecastStatus: ForecastStatus.available,
        radarStatus: RadarStatus.available
    )

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // 1. Status Hero Banner Card
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Image(systemName: isOfficeWifiConnected ? "wifi.circle.fill" : "wifi.slash")
                                .font(.title2)
                                .foregroundColor(isOfficeWifiConnected ? .green : .orange)
                            
                            VStack(alignment: .leading) {
                                Text(isOfficeWifiConnected ? "Office Wi-Fi Connected" : "Not on Office Network")
                                    .font(.headline)
                                Text("SSID: \(currentWifiSsid)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            
                            Text("PUNCTUAL")
                                .font(.caption2)
                                .bold()
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color.green.opacity(0.15))
                                .foregroundColor(.green)
                                .cornerRadius(6)
                        }
                        
                        Divider()
                        
                        HStack {
                            VStack(alignment: .leading) {
                                Text("TODAY'S STATUS")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                                Text(attendanceStatus)
                                    .font(.title3)
                                    .bold()
                                    .foregroundColor(.primary)
                            }
                            Spacer()
                            
                            Button(action: {
                                isOfficeWifiConnected.toggle()
                            }) {
                                Label("Auto Check-In", systemImage: "checkmark.seal.fill")
                                    .font(.subheadline)
                                    .bold()
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 10)
                                    .background(Color.blue)
                                    .foregroundColor(.white)
                                    .cornerRadius(20)
                            }
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(16)
                    .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: 2)

                    // 2. Weather & Travel Advisory Card
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Image(systemName: "sun.max.fill")
                                .foregroundColor(.orange)
                                .font(.title2)
                            Text("Commute Weather Advisory")
                                .font(.headline)
                            Spacer()
                            Text(sampleWeather.commuteStatus.label)
                                .font(.caption)
                                .bold()
                                .foregroundColor(.blue)
                        }
                        
                        HStack(spacing: 16) {
                            VStack(alignment: .leading) {
                                Text("\(sampleWeather.currentTempC)°C")
                                    .font(.system(size: 36, weight: .bold))
                                Text(sampleWeather.condition.label)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            
                            VStack(alignment: .trailing, spacing: 4) {
                                Text(sampleWeather.insight.headline)
                                    .font(.subheadline)
                                    .bold()
                                Text(sampleWeather.insight.detail)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.trailing)
                            }
                        }
                    }
                    .padding()
                    .background(Color.blue.opacity(0.06))
                    .cornerRadius(16)

                    // 3. Upcoming Holiday Card
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            HStack {
                                Text("UPCOMING HOLIDAY")
                                    .font(.caption2)
                                    .bold()
                                    .foregroundColor(.orange)
                                Spacer()
                                Text("LONG WEEKEND")
                                    .font(.caption2)
                                    .bold()
                                    .padding(.horizontal, 6)
                                    .padding(.vertical, 2)
                                    .background(Color.orange.opacity(0.2))
                                    .foregroundColor(.orange)
                                    .cornerRadius(4)
                            }
                            Text("Gandhi Jayanti")
                                .font(.headline)
                            Text("Friday, October 2")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(16)
                    .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: 2)
                }
                .padding()
            }
            .navigationTitle("📍 PingPin")
            .background(Color(.systemGroupedBackground))
        }
    }
}

#Preview {
    HomeView()
}
