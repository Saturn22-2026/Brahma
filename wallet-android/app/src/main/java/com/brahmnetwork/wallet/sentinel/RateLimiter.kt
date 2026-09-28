package com.brahmnetwork.wallet.sentinel

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sliding-window rate limiter — imported from MY NETWORK
 * (`com.sentinal.network.mesh.RateLimiter`).
 */
class RateLimiter(
    private val maxPacketsPerWindow: Int = 100,
    private val windowMillis: Long = 60_000
) {
    private val nodeWindows = ConcurrentHashMap<String, Window>()

    fun isAllowed(nodeId: String): Boolean {
        val now = System.currentTimeMillis()
        val window = nodeWindows.getOrPut(nodeId) { Window(now) }
        synchronized(window) {
            if (now - window.startTime > windowMillis) {
                window.startTime = now
                window.count.set(0)
            }
            return if (window.count.get() < maxPacketsPerWindow) {
                window.count.incrementAndGet()
                true
            } else {
                false
            }
        }
    }

    private class Window(var startTime: Long) {
        val count = AtomicInteger(0)
    }
}
