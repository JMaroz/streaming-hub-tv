package it.streaminghub.tv.data.api

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import it.streaminghub.tv.data.model.StatusDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

sealed interface DiscoveryState {
    data object Idle : DiscoveryState
    data class Searching(val currentTarget: String, val scanned: Int, val total: Int) : DiscoveryState
    data class Found(val url: String, val status: StatusDto) : DiscoveryState
    data class Failed(val message: String) : DiscoveryState
}

class ServerDiscoveryManager {

    suspend fun testConnection(baseUrl: String): Result<StatusDto> = withContext(Dispatchers.IO) {
        try {
            val api = ApiClient.create(baseUrl)
            val status = api.getStatus()
            if (status.status == "online" || status.appName.contains("Streaming Hub", ignoreCase = true)) {
                Result.success(status)
            } else {
                Result.failure(IllegalStateException("Il server ha risposto ma non è Streaming Hub."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun discoverServer(): Flow<DiscoveryState> = flow {
        emit(DiscoveryState.Searching("Inizializzazione scansione di rete…", 0, 0))

        // 1. Check primary candidates (mDNS and common hostnames)
        val priorityCandidates = listOf(
            "http://homeassistant.local:8099",
            "http://homeassistant:8099",
            "http://127.0.0.1:8099"
        )

        for (candidate in priorityCandidates) {
            emit(DiscoveryState.Searching(candidate, 0, priorityCandidates.size))
            val testResult = testConnection(candidate)
            if (testResult.isSuccess) {
                emit(DiscoveryState.Found(candidate, testResult.getOrThrow()))
                return@flow
            }
        }

        // 2. Discover local subnet IP candidates
        val localIps = getLocalIpSubnetCandidates()
        if (localIps.isEmpty()) {
            emit(DiscoveryState.Failed("Nessun indirizzo IP di rete locale valido rilevato sulla TV."))
            return@flow
        }

        val total = localIps.size
        var scannedCount = 0

        // Process in batches of 15 concurrent probes for quick LAN scan using Ktor
        val batches = localIps.chunked(15)
        for (batch in batches) {
            val foundMatch = withContext(Dispatchers.IO) {
                val deferredList = batch.map { ip ->
                    val candidateUrl = "http://$ip:8099"
                    async {
                        val isAlive = probeUrl(candidateUrl)
                        if (isAlive) candidateUrl else null
                    }
                }
                val results = deferredList.awaitAll()
                results.firstOrNull { it != null }
            }

            scannedCount += batch.size
            emit(DiscoveryState.Searching("Scansione IP locali (${scannedCount}/$total)…", scannedCount, total))

            if (foundMatch != null) {
                val fullTest = testConnection(foundMatch)
                if (fullTest.isSuccess) {
                    emit(DiscoveryState.Found(foundMatch, fullTest.getOrThrow()))
                    return@flow
                }
            }
        }

        emit(DiscoveryState.Failed("Nessun server Streaming Hub rilevato sulla porta 8099."))
    }.flowOn(Dispatchers.IO)

    private suspend fun probeUrl(url: String): Boolean {
        return try {
            val response = ApiClient.httpClient.get("$url/api/status")
            response.status == HttpStatusCode.OK
        } catch (_: Exception) {
            false
        }
    }

    private fun getLocalIpSubnetCandidates(): List<String> {
        val candidates = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            for (iface in interfaces) {
                if (iface.isLoopback || !iface.isUp) continue
                for (addr in iface.inetAddresses.toList()) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val hostAddress = addr.hostAddress ?: continue
                        val parts = hostAddress.split(".")
                        if (parts.size == 4) {
                            val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                            val commonHosts = listOf(1, 2, 3, 4, 5, 10, 20, 50, 100, 101, 150, 200)
                            for (host in commonHosts) {
                                candidates.add("$prefix.$host")
                            }
                            for (host in 1..254) {
                                val ip = "$prefix.$host"
                                if (!candidates.contains(ip)) {
                                    candidates.add(ip)
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // fallback
        }
        return candidates
    }
}
