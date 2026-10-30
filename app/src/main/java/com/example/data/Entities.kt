package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_logs")
data class PrayerLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // YYYY-MM-DD
    val prayerName: String, // Fajr, Dhuhr, Asr, Maghrib, Isha
    val status: String // Prayed, Qada, NotPrayed
)

@Entity(tableName = "qada_tallies")
data class QadaTally(
    @PrimaryKey val prayerName: String, // Fajr, Dhuhr, Asr, Maghrib, Isha
    val count: Int
)

@Entity(tableName = "favorite_quotes")
data class FavoriteQuote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val arabic: String,
    val english: String,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ramadan_records")
data class RamadanDayRecord(
    @PrimaryKey val ramadanDay: Int, // 1 to 30
    val fastingStatus: String = "None", // "None", "Fasted", "Qada", "Excused"
    val quranJuzRead: Int = 0, // 0 to 30
    val readMainDua: Boolean = false,
    val readSuhurDua: Boolean = false,
    val readIftarDua: Boolean = false,
    val nightPrayers: Boolean = false,
    val charitySadaqahCount: Int = 0,
    val laylatulQadrCompleted: Boolean = false
)

@Entity(tableName = "tasbeeh_daily")
data class TasbeehDailyRecord(
    @PrimaryKey val dateString: String, // YYYY-MM-DD
    val totalCount: Int
)

@Entity(tableName = "ziyarats")
data class ZiyaratItem(
    @PrimaryKey val id: String,
    val masoomIndex: Int, // 1 to 14
    val titleEn: String,
    val titleAr: String,
    val titleUr: String,
    val audioUrl: String,
    // Legacy Room column retained for schema compatibility. Annual dates come from islamic_events.json.
    val dateStringHijri: String = "",
    val arText: String,
    val enText: String,
    val urText: String,
    val isFavorite: Boolean = false
)

@Entity(tableName = "ziyarat_visits")
data class ZiyaratVisitRecord(
    @PrimaryKey val siteId: String,
    val virtuallyVisited: Boolean = false,
    val physicallyVisited: Boolean = false,
    val visitTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ziyarat_journals")
data class ZiyaratJournalRecord(
    @PrimaryKey val siteId: String,
    val visitDate: String = "",
    val notes: String = "",
    val duaRequests: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "aamal_custom_activities")
data class AamalCustomActivity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String = "Custom",
    val isEnabled: Boolean = true,
    val dateAdded: String
)

@Entity(tableName = "aamal_completions", primaryKeys = ["dateString", "activityId"])
data class AamalCompletion(
    val dateString: String,
    val activityId: String,
    val isCompleted: Boolean,
    val completionCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "hajj_umrah_checklist", primaryKeys = ["type", "stepId"])
data class HajjUmrahChecklistItem(
    val type: String, // "HAJJ" or "UMRAH"
    val stepId: Int,
    val isCompleted: Boolean
)

@Entity(tableName = "my_collections")
data class MyCollection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val duaIds: String = "", // comma-separated ShiaDua IDs
    val ziyaratIds: String = "", // comma-separated ZiyaratItem IDs
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "dua_reading_logs")
data class DuaReadingLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val duaId: String,
    val dateString: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis()
)

// end of entities

@Entity(tableName = "finance_transactions")
data class FinanceTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // YYYY-MM-DD
    val type: String, // Khums, Zakat, Fitrana, Qurbani, Sadaqah, Donation
    val amount: Double,
    val currency: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
