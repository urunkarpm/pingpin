import SwiftUI
import shared

struct HolidaysView: View {
    @State private: var searchText: String = ""
    
    let sampleHolidays = [
        IndianHoliday(
            id: "1",
            name: "Gandhi Jayanti",
            dateYyyyMmDd: "2026-10-02",
            dayOfWeek: "Friday",
            category: HolidayCategory.national,
            description: "National Holiday observing Mahatma Gandhi's birth anniversary.",
            isLongWeekendOverride: true
        ),
        IndianHoliday(
            id: "2",
            name: "Dussehra (Vijayadashami)",
            dateYyyyMmDd: "2026-10-20",
            dayOfWeek: "Tuesday",
            category: HolidayCategory.gazetted,
            description: "Victory of good over evil.",
            isLongWeekendOverride: false
        ),
        IndianHoliday(
            id: "3",
            name: "Diwali (Deepavali)",
            dateYyyyMmDd: "2026-11-08",
            dayOfWeek: "Sunday",
            category: HolidayCategory.gazetted,
            description: "Festival of Lights.",
            isLongWeekendOverride: false
        )
    ]

    var body: some View {
        NavigationStack {
            List(sampleHolidays, id: \.id) { holiday in
                VStack(alignment: .leading, spacing: 6) {
                    HStack {
                        Text(holiday.name)
                            .font(.headline)
                        Spacer()
                        if holiday.isLongWeekend {
                            Text("LONG WEEKEND")
                                .font(.caption2)
                                .bold()
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.orange.opacity(0.15))
                                .foregroundColor(.orange)
                                .cornerRadius(4)
                        }
                    }
                    
                    HStack {
                        Label("\(holiday.dayOfWeek), \(holiday.dateYyyyMmDd)", systemImage: "calendar")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Spacer()
                        Text(holiday.category.label)
                            .font(.caption2)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.blue.opacity(0.12))
                            .foregroundColor(.blue)
                            .cornerRadius(4)
                    }
                    
                    Text(holiday.description)
                        .font(.caption)
                        .foregroundColor(.gray)
                }
                .padding(.vertical, 4)
            }
            .navigationTitle("Holidays (2026)")
            .searchable(text: $searchText, prompt: "Search Indian holidays...")
        }
    }
}

#Preview {
    HolidaysView()
}
