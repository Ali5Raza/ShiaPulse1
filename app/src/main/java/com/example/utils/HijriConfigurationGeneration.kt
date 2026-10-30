package com.example.utils

import java.util.concurrent.atomic.AtomicLong

/** Invalidates delayed configuration results across activities and service instances. */
internal object HijriConfigurationGeneration {
    private val generation = AtomicLong(0L)
    private val monitor = Any()

    fun begin(): Long = synchronized(monitor) { generation.incrementAndGet() }
    fun invalidate(): Long = synchronized(monitor) { generation.incrementAndGet() }
    fun current(): Long = generation.get()
    fun isCurrent(token: Long): Boolean = generation.get() == token

    /** Runs only synchronous cache/preference/global-state writes, never suspending work. */
    fun runIfCurrent(token: Long, block: () -> Unit): Boolean = synchronized(monitor) {
        if (generation.get() != token) {
            false
        } else {
            block()
            true
        }
    }
}
