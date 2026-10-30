package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PrayerLog::class,
        QadaTally::class,
        FavoriteQuote::class,
        RamadanDayRecord::class,
        TasbeehDailyRecord::class,
        ZiyaratItem::class,
        ZiyaratVisitRecord::class,
        ZiyaratJournalRecord::class,
        AamalCustomActivity::class,
        AamalCompletion::class,
        HajjUmrahChecklistItem::class,
        MyCollection::class,
        DuaReadingLog::class,
        FinanceTransaction::class
    ],
    version = 14,
    exportSchema = true
)
abstract class PrayerDatabase : RoomDatabase() {
    abstract fun prayerDao(): PrayerDao

    companion object {
        @Volatile
        private var INSTANCE: PrayerDatabase? = null

        fun getDatabase(context: Context): PrayerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrayerDatabase::class.java,
                    "shia_prayer_database"
                )
                    // Unknown/unrecovered versions must fail rather than silently erase user data.
                    .addMigrations(*PrayerMigrations.ALL)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
