package com.tenesuzun.atvrnd.ui.components

import java.net.InetSocketAddress
import java.net.Socket

object NetworkSpeedAnalyzer {

    // Enum for network types with associated bitrate (in bits per second)
    enum class NetworkType(val bitRate: Double) {
        WIFI(2000000.0),
        FOUR_G(1000000.0),
        THREE_G(500000.0),
        EDGE(100000.0),
        GPRS(25000.0);

        companion object {
            // Map measured latency (in milliseconds) to a network type
            fun from(latency: Double): NetworkType {
                return when {
                    latency < 50 -> WIFI
                    latency < 150 -> FOUR_G
                    latency < 300 -> THREE_G
                    latency < 600 -> EDGE
                    else -> GPRS
                }
            }

            // Determine pre-buffer time based on network type
            fun calculatePreBufferTime(networkType: NetworkType, latency: Double): Double {
                return when (networkType) {
                    WIFI, FOUR_G -> 2.0
                    else -> 1.0
                }
            }
        }
    }

    /**
     * Measures the network latency and then calculates the optimized bitrate and pre-buffer time.
     * @param callback Returns a pair (bitRate in bits per second, preBufferTime in seconds)
     */
    fun optimizedBitRate(callback: (bitRate: Double, preBufferTime: Double) -> Unit) {
        measureNetworkLatency { latency ->
            val networkType = NetworkType.from(latency)
            val preBufferTime = NetworkType.calculatePreBufferTime(networkType, latency)
            callback(networkType.bitRate, preBufferTime)
        }
    }

    /**
     * Measures network latency by opening a TCP connection to google.com on port 80.
     * This method runs on a background thread.
     * @param callback Returns the measured latency in milliseconds.
     */
    private fun measureNetworkLatency(callback: (latency: Double) -> Unit) {
        Thread {
            val latency = performLatencyTest()
            callback(latency)
        }.start()
    }

    /**
     * Performs a latency test by creating a socket connection.
     * @return the latency in milliseconds. If the connection fails, a default high latency is returned.
     */
    private fun performLatencyTest(): Double {
        val host = "google.com"
        val port = 80
        return try {
            val startTime = System.currentTimeMillis()
            Socket().use { socket ->
                // Connect with a timeout of 5000 ms
                socket.connect(InetSocketAddress(host, port), 5000)
            }
            val latency = System.currentTimeMillis() - startTime
            latency.toDouble()
        } catch (e: Exception) {
            e.printStackTrace()
            500.0  // Default to 500 ms if connection fails
        }
    }
}