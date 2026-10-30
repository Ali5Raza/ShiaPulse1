package com.example.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrations for Room versions that are evidenced by mainline source history.
 *
 * Versions 1, 2, 5, 7, 9 and 12 have no recovered schema evidence. No migration is
 * fabricated for them; without a destructive fallback they fail closed instead of wiping data.
 */
internal object PrayerMigrations {
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `ziyarats` (`id` TEXT NOT NULL, `masoomIndex` INTEGER NOT NULL, `titleEn` TEXT NOT NULL, `titleAr` TEXT NOT NULL, `titleUr` TEXT NOT NULL, `audioUrl` TEXT NOT NULL, `dateStringHijri` TEXT NOT NULL, `arText` TEXT NOT NULL, `enText` TEXT NOT NULL, `urText` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, PRIMARY KEY(`id`))"""
            )
        }
    }

    private val MIGRATION_4_6 = object : Migration(4, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `ziyarat_visits` (`siteId` TEXT NOT NULL, `virtuallyVisited` INTEGER NOT NULL, `physicallyVisited` INTEGER NOT NULL, `visitTimestamp` INTEGER NOT NULL, PRIMARY KEY(`siteId`))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `ziyarat_journals` (`siteId` TEXT NOT NULL, `visitDate` TEXT NOT NULL, `notes` TEXT NOT NULL, `duaRequests` TEXT NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`siteId`))"""
            )
        }
    }

    // Mainline source between versions 6 and 8 has no entity or column change.
    private val MIGRATION_6_8 = object : Migration(6, 8) {
        override fun migrate(db: SupportSQLiteDatabase) = Unit
    }

    private val MIGRATION_8_10 = object : Migration(8, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `aamal_custom_activities` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `category` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `dateAdded` TEXT NOT NULL, PRIMARY KEY(`id`))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `aamal_completions` (`dateString` TEXT NOT NULL, `activityId` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `completionCount` INTEGER NOT NULL, PRIMARY KEY(`dateString`, `activityId`))"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `hajj_umrah_checklist` (`type` TEXT NOT NULL, `stepId` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, PRIMARY KEY(`type`, `stepId`))"""
            )
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 10 stored no completion time. Zero marks that value as unknown; using the
            // migration clock would invent a completion timestamp and bias learned reminder hours.
            db.execSQL(
                "ALTER TABLE `aamal_completions` " +
                    "ADD COLUMN `timestamp` INTEGER NOT NULL DEFAULT 0"
            )
        }
    }

    private val MIGRATION_11_13 = object : Migration(11, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `my_collections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `duaIds` TEXT NOT NULL, `ziyaratIds` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"""
            )
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `dua_reading_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `duaId` TEXT NOT NULL, `dateString` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"""
            )
        }
    }

    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS `finance_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateString` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `currency` TEXT NOT NULL, `notes` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"""
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_3_4,
        MIGRATION_4_6,
        MIGRATION_6_8,
        MIGRATION_8_10,
        MIGRATION_10_11,
        MIGRATION_11_13,
        MIGRATION_13_14
    )
}
