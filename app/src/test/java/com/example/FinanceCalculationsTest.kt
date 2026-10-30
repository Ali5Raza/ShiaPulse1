package com.example

import com.example.data.FinanceTransaction
import com.example.data.calculateCurrencyTotals
import com.example.data.calculateTransactionTotal
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceCalculationsTest {
    private val transactions = listOf(
        FinanceTransaction(dateString = "2026-10-20", type = "Sadaqah", amount = 10.0, currency = "\$"),
        FinanceTransaction(dateString = "2026-10-21", type = "Sadaqah", amount = 5.0, currency = "\$"),
        FinanceTransaction(dateString = "2026-10-21", type = "Khums", amount = 2000.0, currency = "PKR"),
        FinanceTransaction(dateString = "2026-10-22", type = "Donation", amount = 500.0, currency = ""),
        FinanceTransaction(dateString = "2026-10-23", type = "Sadaqah", amount = 50.0, currency = "AED")
    )

    @Test
    fun totalsNeverMixCurrencies() {
        val totals = calculateCurrencyTotals(transactions, fallbackCurrency = "PKR")

        assertEquals(15.0, totals.getValue("\$"), 0.0001)
        assertEquals(2500.0, totals.getValue("PKR"), 0.0001)
        assertEquals(50.0, totals.getValue("AED"), 0.0001)
    }

    @Test
    fun persistedSadaqahTotalUsesOnlyTheDisplayedCurrency() {
        assertEquals(
            15.0,
            calculateTransactionTotal(transactions, type = "Sadaqah", currency = "\$"),
            0.0001
        )
        assertEquals(
            50.0,
            calculateTransactionTotal(transactions, type = "sadaqah", currency = "aed"),
            0.0001
        )
        assertEquals(
            0.0,
            calculateTransactionTotal(transactions, type = "Sadaqah", currency = "PKR"),
            0.0001
        )
    }
}
