package dev.nocturne.ambient.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Http {
    suspend fun getText(
        url: String,
        timeoutMs: Int = 12_000,
        maxBytes: Int = 4 * 1024 * 1024,
        accept: String = "application/json"
    ): String = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                useCaches = false
                setRequestProperty("Accept", accept)
                setRequestProperty("User-Agent", "Nocturne/1.0 (Fire TV ambient clock)")
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("HTTP $code from ${URL(url).host}")
            }
            val input = connection.inputStream
            val bytes = input.use { stream ->
                val buffer = ByteArray(16 * 1024)
                val out = java.io.ByteArrayOutputStream()
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    if (out.size() > maxBytes) throw IOException("Response too large")
                }
                out.toByteArray()
            }
            bytes.toString(Charsets.UTF_8)
        } finally {
            connection?.disconnect()
        }
    }
}
