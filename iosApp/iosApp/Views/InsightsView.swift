import SwiftUI
import shared

struct InsightsView: View {
    @State private var attendanceRate: Double = 92.5
    @State private var totalWfoDays: Int = 18
    @State private var autoVerificationPercent: Int = 88

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Executive Header Card
                    VStack(alignment: .leading, spacing: 8) {
                        Text("EXECUTIVE DASHBOARD")
                            .font(.caption2)
                            .bold()
                            .foregroundColor(.blue)
                        Text("Attendance Digest")
                            .font(.title2)
                            .bold()
                        Text("100% On-Device Analytics Report")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(16)
                    .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: 2)

                    // KPI Grid Stat Cards
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Image(systemName: "chart.bar.fill")
                                    .foregroundColor(.blue)
                                Spacer()
                                Text("TARGET 85%")
                                    .font(.caption2)
                                    .foregroundColor(.secondary)
                            }
                            Text("\(String(format: "%.1f", attendanceRate))%")
                                .font(.title)
                                .bold()
                            Text("WFO Attendance Rate")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color.blue.opacity(0.06))
                        .cornerRadius(16)

                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Image(systemName: "checkmark.seal.fill")
                                    .foregroundColor(.green)
                                Spacer()
                                Text("COMPLIANT")
                                    .font(.caption2)
                                    .foregroundColor(.green)
                            }
                            Text("100%")
                                .font(.title)
                                .bold()
                            Text("Policy Compliance")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color.green.opacity(0.06))
                        .cornerRadius(16)

                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Image(systemName: "building.2.fill")
                                    .foregroundColor(.purple)
                                Spacer()
                            }
                            Text("\(totalWfoDays) Days")
                                .font(.title)
                                .bold()
                            Text("Office Days Attended")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color.purple.opacity(0.06))
                        .cornerRadius(16)

                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Image(systemName: "wifi.circle.fill")
                                    .foregroundColor(.orange)
                                Spacer()
                            }
                            Text("\(autoVerificationPercent)%")
                                .font(.title)
                                .bold()
                            Text("Auto Wi-Fi Logged")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color.orange.opacity(0.06))
                        .cornerRadius(16)
                    }

                    // Smart Narrative Commentary Digest Card
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            Image(systemName: "doc.text.image.fill")
                                .foregroundColor(.blue)
                            Text("Smart Narrative Digest")
                                .font(.headline)
                        }
                        Divider()
                        Text("Outstanding attendance performance this month! You have fulfilled 18 out of 20 mandatory office days. 88% of arrivals were automatically verified via office Wi-Fi detection.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .lineSpacing(4)
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(16)
                    .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: 2)

                    // Export PDF Button
                    Button(action: {
                        // Triggers Native PDF Report Generator
                    }) {
                        HStack {
                            Image(systemName: "square.and.arrow.up.fill")
                            Text("Export Executive PDF Statement")
                                .bold()
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(14)
                    }
                }
                .padding()
            }
            .navigationTitle("Analytics & PDF")
            .background(Color(.systemGroupedBackground))
        }
    }
}

#Preview {
    InsightsView()
}
