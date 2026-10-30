package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Opens each evidenced historical schema with the real current Room database.
 *
 * Fixtures come from historical entity source and recovered debug-APK SQL, not guessed
 * MigrationTestHelper JSON. Opening v14 forces Room's own final schema validation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class PrayerDatabaseMigrationTest {
    private lateinit var context: Context
    private val databaseNames = mutableSetOf<String>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        databaseNames.forEach { context.deleteDatabase(it) }
        databaseNames.clear()
    }

    @Test
    fun migrate3To14PreservesHistoricalRows() = assertMigrates(3)

    @Test
    fun migrate4To14PreservesHistoricalRows() = assertMigrates(4)

    @Test
    fun migrate6To14PreservesHistoricalRows() = assertMigrates(6)

    @Test
    fun migrate8To14PreservesHistoricalRows() = assertMigrates(8)

    @Test
    fun migrate10To14PreservesCompletionWithUnknownTimestamp() = assertMigrates(10)

    @Test
    fun migrate11To14PreservesKnownCompletionTimestamp() = assertMigrates(11)

    @Test
    fun migrate13To14PreservesHistoricalRows() = assertMigrates(13)

    private fun assertMigrates(startVersion: Int) {
        val name = "prayer-migration-$startVersion.db"
        databaseNames += name
        context.deleteDatabase(name)
        createLegacyDatabase(name, startVersion)
        var database: PrayerDatabase? = null

        try {
            val openedDatabase = Room.databaseBuilder(context, PrayerDatabase::class.java, name)
                .addMigrations(*PrayerMigrations.ALL)
                .allowMainThreadQueries()
                .build()
            database = openedDatabase
            val migrated = openedDatabase.openHelper.writableDatabase

            assertEquals(14, migrated.version)
            TABLE_INTRODUCTIONS.forEach { (table, introducedVersion) ->
                val expectedRows = if (startVersion >= introducedVersion) 1L else 0L
                assertEquals(
                    "Unexpected row count for $table when migrating from v$startVersion",
                    expectedRows,
                    migrated.longValue("SELECT COUNT(*) FROM `$table`")
                )
            }
            assertEquals(
                "Prayed",
                migrated.stringValue(
                    "SELECT status FROM prayer_logs WHERE dateString = '2026-01-01'"
                )
            )

            when {
                startVersion == 10 -> assertEquals(
                    0L,
                    migrated.longValue(COMPLETION_TIMESTAMP_QUERY)
                )
                startVersion >= 11 -> assertEquals(
                    KNOWN_COMPLETION_TIMESTAMP,
                    migrated.longValue(COMPLETION_TIMESTAMP_QUERY)
                )
            }
        } finally {
            database?.close()
            context.deleteDatabase(name)
            databaseNames -= name
        }
    }

    private fun createLegacyDatabase(name: String, version: Int) {
        val database = context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null)
        try {
            TABLE_INTRODUCTIONS.forEach { (table, introducedVersion) ->
                if (version >= introducedVersion && version < 14) {
                    database.execSQL(createTableSql(table, version))
                    seedRow(database, table, version)
                }
            }
            database.version = version
        } finally {
            database.close()
        }
    }

    private fun createTableSql(table: String, version: Int): String = when (table) {
        "prayer_logs" ->
            """CREATE TABLE IF NOT EXISTS `prayer_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateString` TEXT NOT NULL, `prayerName` TEXT NOT NULL, `status` TEXT NOT NULL)"""
        "qada_tallies" ->
            """CREATE TABLE IF NOT EXISTS `qada_tallies` (`prayerName` TEXT NOT NULL, `count` INTEGER NOT NULL, PRIMARY KEY(`prayerName`))"""
        "favorite_quotes" ->
            """CREATE TABLE IF NOT EXISTS `favorite_quotes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `arabic` TEXT NOT NULL, `english` TEXT NOT NULL, `source` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"""
        "ramadan_records" ->
            """CREATE TABLE IF NOT EXISTS `ramadan_records` (`ramadanDay` INTEGER NOT NULL, `fastingStatus` TEXT NOT NULL, `quranJuzRead` INTEGER NOT NULL, `readMainDua` INTEGER NOT NULL, `readSuhurDua` INTEGER NOT NULL, `readIftarDua` INTEGER NOT NULL, `nightPrayers` INTEGER NOT NULL, `charitySadaqahCount` INTEGER NOT NULL, `laylatulQadrCompleted` INTEGER NOT NULL, PRIMARY KEY(`ramadanDay`))"""
        "tasbeeh_daily" ->
            """CREATE TABLE IF NOT EXISTS `tasbeeh_daily` (`dateString` TEXT NOT NULL, `totalCount` INTEGER NOT NULL, PRIMARY KEY(`dateString`))"""
        "ziyarats" ->
            """CREATE TABLE IF NOT EXISTS `ziyarats` (`id` TEXT NOT NULL, `masoomIndex` INTEGER NOT NULL, `titleEn` TEXT NOT NULL, `titleAr` TEXT NOT NULL, `titleUr` TEXT NOT NULL, `audioUrl` TEXT NOT NULL, `dateStringHijri` TEXT NOT NULL, `arText` TEXT NOT NULL, `enText` TEXT NOT NULL, `urText` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, PRIMARY KEY(`id`))"""
        "ziyarat_visits" ->
            """CREATE TABLE IF NOT EXISTS `ziyarat_visits` (`siteId` TEXT NOT NULL, `virtuallyVisited` INTEGER NOT NULL, `physicallyVisited` INTEGER NOT NULL, `visitTimestamp` INTEGER NOT NULL, PRIMARY KEY(`siteId`))"""
        "ziyarat_journals" ->
            """CREATE TABLE IF NOT EXISTS `ziyarat_journals` (`siteId` TEXT NOT NULL, `visitDate` TEXT NOT NULL, `notes` TEXT NOT NULL, `duaRequests` TEXT NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`siteId`))"""
        "aamal_custom_activities" ->
            """CREATE TABLE IF NOT EXISTS `aamal_custom_activities` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `category` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `dateAdded` TEXT NOT NULL, PRIMARY KEY(`id`))"""
        "aamal_completions" -> {
            val timestampColumn = if (version >= 11) ", `timestamp` INTEGER NOT NULL" else ""
            """CREATE TABLE IF NOT EXISTS `aamal_completions` (`dateString` TEXT NOT NULL, `activityId` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `completionCount` INTEGER NOT NULL$timestampColumn, PRIMARY KEY(`dateString`, `activityId`))"""
        }
        "hajj_umrah_checklist" ->
            """CREATE TABLE IF NOT EXISTS `hajj_umrah_checklist` (`type` TEXT NOT NULL, `stepId` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, PRIMARY KEY(`type`, `stepId`))"""
        "my_collections" ->
            """CREATE TABLE IF NOT EXISTS `my_collections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `duaIds` TEXT NOT NULL, `ziyaratIds` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"""
        "dua_reading_logs" ->
            """CREATE TABLE IF NOT EXISTS `dua_reading_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `duaId` TEXT NOT NULL, `dateString` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"""
        "finance_transactions" ->
            """CREATE TABLE IF NOT EXISTS `finance_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateString` TEXT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `currency` TEXT NOT NULL, `notes` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"""
        else -> error("Unknown fixture table: $table")
    }

    private fun seedRow(database: SQLiteDatabase, table: String, version: Int) {
        val sql = when (table) {
            "prayer_logs" ->
                "INSERT INTO prayer_logs (dateString, prayerName, status) " +
                    "VALUES ('2026-01-01', 'Fajr', 'Prayed')"
            "qada_tallies" ->
                "INSERT INTO qada_tallies (prayerName, count) VALUES ('Fajr', 2)"
            "favorite_quotes" ->
                "INSERT INTO favorite_quotes (arabic, english, source, timestamp) " +
                    "VALUES ('ar', 'en', 'source', 1234)"
            "ramadan_records" ->
                "INSERT INTO ramadan_records (ramadanDay, fastingStatus, quranJuzRead, " +
                    "readMainDua, readSuhurDua, readIftarDua, nightPrayers, " +
                    "charitySadaqahCount, laylatulQadrCompleted) " +
                    "VALUES (1, 'Fasted', 1, 1, 1, 1, 0, 2, 0)"
            "tasbeeh_daily" ->
                "INSERT INTO tasbeeh_daily (dateString, totalCount) VALUES ('2026-01-01', 33)"
            "ziyarats" ->
                "INSERT INTO ziyarats (id, masoomIndex, titleEn, titleAr, titleUr, audioUrl, " +
                    "dateStringHijri, arText, enText, urText, isFavorite) " +
                    "VALUES ('z1', 1, 'en', 'ar', 'ur', '', '1-1-1447', 'ar', 'en', 'ur', 1)"
            "ziyarat_visits" ->
                "INSERT INTO ziyarat_visits (siteId, virtuallyVisited, physicallyVisited, " +
                    "visitTimestamp) VALUES ('z1', 1, 0, 1234)"
            "ziyarat_journals" ->
                "INSERT INTO ziyarat_journals (siteId, visitDate, notes, duaRequests, lastUpdated) " +
                    "VALUES ('z1', '2026-01-01', 'notes', 'duas', 1234)"
            "aamal_custom_activities" ->
                "INSERT INTO aamal_custom_activities (id, title, category, isEnabled, dateAdded) " +
                    "VALUES ('custom1', 'Custom', 'Custom', 1, '2026-01-01')"
            "aamal_completions" -> if (version >= 11) {
                "INSERT INTO aamal_completions (dateString, activityId, isCompleted, " +
                    "completionCount, timestamp) VALUES ('2026-01-01', 'aamal1', 1, 1, " +
                    "$KNOWN_COMPLETION_TIMESTAMP)"
            } else {
                "INSERT INTO aamal_completions (dateString, activityId, isCompleted, " +
                    "completionCount) VALUES ('2026-01-01', 'aamal1', 1, 1)"
            }
            "hajj_umrah_checklist" ->
                "INSERT INTO hajj_umrah_checklist (type, stepId, isCompleted) VALUES ('HAJJ', 1, 1)"
            "my_collections" ->
                "INSERT INTO my_collections (name, description, duaIds, ziyaratIds, timestamp) " +
                    "VALUES ('collection', 'description', 'dua1', 'z1', 1234)"
            "dua_reading_logs" ->
                "INSERT INTO dua_reading_logs (duaId, dateString, timestamp) " +
                    "VALUES ('dua1', '2026-01-01', 1234)"
            "finance_transactions" ->
                "INSERT INTO finance_transactions (dateString, type, amount, currency, notes, " +
                    "timestamp) VALUES ('2026-01-01', 'Khums', 10.0, 'PKR', '', 1234)"
            else -> error("Unknown fixture table: $table")
        }
        database.execSQL(sql)
    }

    private fun SupportSQLiteDatabase.longValue(sql: String): Long = query(sql).use { cursor ->
        assertTrue("No result for: $sql", cursor.moveToFirst())
        cursor.getLong(0)
    }

    private fun SupportSQLiteDatabase.stringValue(sql: String): String = query(sql).use { cursor ->
        assertTrue("No result for: $sql", cursor.moveToFirst())
        cursor.getString(0)
    }

    private companion object {
        const val KNOWN_COMPLETION_TIMESTAMP = 1_234_567_890L
        const val COMPLETION_TIMESTAMP_QUERY =
            "SELECT timestamp FROM aamal_completions " +
                "WHERE dateString = '2026-01-01' AND activityId = 'aamal1'"

        val TABLE_INTRODUCTIONS = linkedMapOf(
            "prayer_logs" to 3,
            "qada_tallies" to 3,
            "favorite_quotes" to 3,
            "ramadan_records" to 3,
            "tasbeeh_daily" to 3,
            "ziyarats" to 4,
            "ziyarat_visits" to 6,
            "ziyarat_journals" to 6,
            "aamal_custom_activities" to 10,
            "aamal_completions" to 10,
            "hajj_umrah_checklist" to 10,
            "my_collections" to 13,
            "dua_reading_logs" to 13,
            "finance_transactions" to 14
        )
    }
}
