package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayer_logs WHERE dateString = :dateString")
    fun getPrayerLogsForDate(dateString: String): Flow<List<PrayerLog>>

    @Query("SELECT * FROM prayer_logs")
    fun getAllPrayerLogs(): Flow<List<PrayerLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerLog(log: PrayerLog)

    @Query("SELECT * FROM qada_tallies")
    fun getAllQadaTallies(): Flow<List<QadaTally>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQadaTally(tally: QadaTally)

    @Query("UPDATE qada_tallies SET count = :count WHERE prayerName = :prayerName")
    suspend fun updateQadaTally(prayerName: String, count: Int)

    @Query("SELECT * FROM favorite_quotes ORDER BY timestamp DESC")
    fun getFavoriteQuotes(): Flow<List<FavoriteQuote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteQuote(quote: FavoriteQuote)

    @Delete
    suspend fun deleteFavoriteQuote(quote: FavoriteQuote)

    @Query("SELECT * FROM ramadan_records ORDER BY ramadanDay ASC")
    fun getAllRamadanRecords(): Flow<List<RamadanDayRecord>>

    @Query("SELECT * FROM ramadan_records WHERE ramadanDay = :ramadanDay LIMIT 1")
    suspend fun getRamadanRecordDay(ramadanDay: Int): RamadanDayRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRamadanRecord(record: RamadanDayRecord)

    @Query("SELECT * FROM tasbeeh_daily ORDER BY dateString ASC")
    fun getAllTasbeehRecords(): Flow<List<TasbeehDailyRecord>>

    @Query("SELECT * FROM tasbeeh_daily WHERE dateString = :dateString LIMIT 1")
    suspend fun getTasbeehRecordForDate(dateString: String): TasbeehDailyRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasbeehRecord(record: TasbeehDailyRecord)

    @Query("SELECT * FROM ziyarats ORDER BY masoomIndex ASC")
    fun getAllZiyarats(): Flow<List<ZiyaratItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZiyarats(ziyarats: List<ZiyaratItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZiyarat(ziyarat: ZiyaratItem)

    @Query("SELECT * FROM ziyarat_visits")
    fun getAllZiyaratVisits(): Flow<List<ZiyaratVisitRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZiyaratVisit(visit: ZiyaratVisitRecord)

    @Query("SELECT * FROM ziyarat_journals")
    fun getAllZiyaratJournals(): Flow<List<ZiyaratJournalRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZiyaratJournal(journal: ZiyaratJournalRecord)

    // Aamal-e-Rozana Queries
    @Query("SELECT * FROM aamal_custom_activities")
    fun getAllCustomAamal(): Flow<List<AamalCustomActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomAamal(activity: AamalCustomActivity)

    @Query("SELECT * FROM aamal_custom_activities WHERE id = :id LIMIT 1")
    suspend fun getCustomAamalById(id: String): AamalCustomActivity?

    @Query("DELETE FROM aamal_custom_activities WHERE id = :id")
    suspend fun deleteCustomAamal(id: String)

    @Query("SELECT * FROM aamal_completions WHERE dateString = :dateString")
    fun getAamalCompletionsForDate(dateString: String): Flow<List<AamalCompletion>>

    @Query("SELECT * FROM aamal_completions")
    fun getAllAamalCompletions(): Flow<List<AamalCompletion>>

    @Query("SELECT * FROM aamal_completions")
    suspend fun getAllAamalCompletionsSync(): List<AamalCompletion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAamalCompletion(completion: AamalCompletion)

    @Query("SELECT * FROM hajj_umrah_checklist")
    fun getAllHajjUmrahItems(): Flow<List<HajjUmrahChecklistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHajjUmrahItem(item: HajjUmrahChecklistItem)

    @Query("SELECT * FROM my_collections ORDER BY timestamp DESC")
    fun getAllCollections(): Flow<List<MyCollection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: MyCollection)

    @Delete
    suspend fun deleteCollection(collection: MyCollection)

    @Query("SELECT * FROM dua_reading_logs ORDER BY timestamp ASC")
    fun getAllDuaReadingLogs(): Flow<List<DuaReadingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuaReadingLog(log: DuaReadingLog)


    // Finance Transactions
    @Query("SELECT * FROM finance_transactions ORDER BY timestamp DESC")
    fun getAllFinanceTransactions(): kotlinx.coroutines.flow.Flow<List<FinanceTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinanceTransaction(transaction: FinanceTransaction)

    @Delete
    suspend fun deleteFinanceTransaction(transaction: FinanceTransaction)
}
