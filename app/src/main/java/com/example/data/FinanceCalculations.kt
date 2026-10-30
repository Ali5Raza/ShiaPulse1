package com.example.data

import java.util.Locale

/** Groups totals by their actual currency instead of presenting a meaningless mixed-currency sum. */
fun calculateCurrencyTotals(
    transactions: Iterable<FinanceTransaction>,
    fallbackCurrency: String
): Map<String, Double> = transactions
    .groupBy { transaction ->
        transaction.currency.ifBlank { fallbackCurrency }.uppercase(Locale.ROOT)
    }
    .mapValues { (_, entries) -> entries.sumOf { it.amount } }

/** Returns a persisted total for one transaction type and one display currency. */
fun calculateTransactionTotal(
    transactions: Iterable<FinanceTransaction>,
    type: String,
    currency: String
): Double = transactions
    .filter { transaction ->
        transaction.type.equals(type, ignoreCase = true) &&
            transaction.currency.ifBlank { currency }.equals(currency, ignoreCase = true)
    }
    .sumOf { it.amount }
