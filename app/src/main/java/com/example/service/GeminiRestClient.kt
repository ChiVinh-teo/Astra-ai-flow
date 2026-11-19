package com.example.service

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class UploadedGeminiFile(
    val fileUri: String,
    val mimeType: String,
    val name: String
)

/**
 * Direct HTTPS Gemini REST client using custom user API key via x-goog-api-key header.
 * Supports:
 * 1) GET /v1beta/models discovery
 * 2) Resumable Gemini Files API upload for PDFs >= 8MB + ACTIVE state polling
 * 3) Structured JSON generation (responseMimeType + responseSchema) & multimodal parts
 */
class GeminiRestClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchAvailableModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Vui lòng nhập Gemini API Key trước khi kiểm tra."))
        }
        runCatching {
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models")
                .header("x-goog-api-key", apiKey.trim())
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException(parseGeminiError(response.code, bodyStr))
                }
                val root = JSONObject(bodyStr)
                val arr = root.optJSONArray("models") ?: JSONArray()
                val usable = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    val methods = m.optJSONArray("supportedGenerationMethods")
                    var supportsGen = false
                    if (methods != null) {
                        for (j in 0 until methods.length()) {
                            if (methods.optString(j) == "generateContent") {
                                supportsGen = true
                                break
                            }
                        }
                    }
                    val rawName = m.optString("name").removePrefix("models/")
                    if (supportsGen && rawName.contains("gemini", ignoreCase = true)) {
                        usable += rawName
                    }
                }
                if (usable.isEmpty()) {
                    throw IOException("Kết nối thành công nhưng không tìm thấy model Gemini hỗ trợ generateContent.")
                }
                usable.sortedByDescending {
                    when {
                        it.contains("2.5-flash") && !it.contains("lite") -> 100
                        it.contains("2.5-pro") -> 95
                        it.contains("2.5") -> 90
                        else -> 10
                    }
                }
            }
        }
    }

    /**
     * Streams PDF >= 8MB directly from ContentResolver to Gemini Files API without duplicating base64 in RAM,
     * then polls GET /v1beta/{name} until state == "ACTIVE".
     */
    suspend fun uploadLargePdfAndWaitActive(
        context: Context,
        apiKey: String,
        uri: Uri,
        displayName: String,
        sizeBytes: Long,
        onStatus: (String) -> Unit
    ): UploadedGeminiFile = withContext(Dispatchers.IO) {
        onStatus("Đang khởi tạo phiên tải tệp lớn lên Gemini Files API...")
        val metaJson = JSONObject().apply {
            put("file", JSONObject().apply { put("display_name", displayName) })
        }
        val startReq = Request.Builder()
            .url("https://generativelanguage.googleapis.com/upload/v1beta/files")
            .header("x-goog-api-key", apiKey.trim())
            .header("X-Goog-Upload-Protocol", "resumable")
            .header("X-Goog-Upload-Command", "start")
            .header("X-Goog-Upload-Header-Content-Length", sizeBytes.toString())
            .header("X-Goog-Upload-Header-Content-Type", "application/pdf")
            .post(metaJson.toString().toRequestBody(jsonMediaType))
            .build()

        val uploadUrl = httpClient.newCall(startReq).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException(parseGeminiError(resp.code, resp.body?.string().orEmpty()))
            }
            resp.header("X-Goog-Upload-URL")
                ?: throw IOException("Không nhận được đường dẫn tải lên từ Gemini Files API.")
        }

        onStatus("Đang truyền luồng PDF an toàn bộ nhớ lên máy chủ Gemini...")
        val streamBody = object : RequestBody() {
            override fun contentType() = "application/pdf".toMediaType()
            override fun contentLength(): Long = if (sizeBytes > 0) sizeBytes else -1L
            override fun writeTo(sink: BufferedSink) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                    }
                } ?: throw IOException("Không thể mở luồng đọc tệp PDF.")
            }
        }

        val uploadReq = Request.Builder()
            .url(uploadUrl)
            .header("X-Goog-Upload-Offset", "0")
            .header("X-Goog-Upload-Command", "upload, finalize")
            .post(streamBody)
            .build()

        val uploaded = httpClient.newCall(uploadReq).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException(parseGeminiError(resp.code, bodyStr))
            val fileObj = JSONObject(bodyStr).optJSONObject("file") ?: JSONObject(bodyStr)
            UploadedGeminiFile(
                fileUri = fileObj.getString("uri"),
                mimeType = fileObj.optString("mimeType", "application/pdf"),
                name = fileObj.optString("name")
            )
        }

        // Poll file state until ACTIVE before invoking generateContent
        onStatus("Đang chờ Gemini lập chỉ mục tài liệu PDF (trạng thái ACTIVE)...")
        val pollUrl = if (uploaded.name.startsWith("files/")) {
            "https://generativelanguage.googleapis.com/v1beta/${uploaded.name}"
        } else {
            uploaded.fileUri
        }
        repeat(15) { attempt ->
            val pollReq = Request.Builder()
                .url(pollUrl)
                .header("x-goog-api-key", apiKey.trim())
                .get()
                .build()
            val state = httpClient.newCall(pollReq).execute().use { resp ->
                if (!resp.isSuccessful) "PROCESSING"
                else JSONObject(resp.body?.string().orEmpty()).optString("state", "ACTIVE")
            }
            if (state.equals("ACTIVE", ignoreCase = true)) {
                return@withContext uploaded
            }
            if (state.equals("FAILED", ignoreCase = true)) {
                throw IOException("Gemini không thể xử lý tệp PDF này (trạng thái FAILED).")
            }
            onStatus("Đang xử lý PDF trên máy chủ (${attempt + 1}/15)...")
            delay(1500L)
        }
        uploaded
    }

    suspend fun generateContent(
        apiKey: String,
        modelId: String,
        systemInstruction: String?,
        contents: JSONArray,
        responseSchema: JSONObject? = null,
        temperature: Double = 0.4
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Chưa có Gemini API Key. Hãy mở Cài đặt AI để dán khóa API của bạn.")
            )
        }
        runCatching {
            val cleanModel = modelId.trim().removePrefix("models/").ifBlank { "gemini-2.5-flash" }
            val payload = JSONObject().apply {
                if (!systemInstruction.isNullOrBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                    })
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", temperature)
                    if (responseSchema != null) {
                        put("responseMimeType", "application/json")
                        put("responseSchema", responseSchema)
                    }
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent")
                .header("x-goog-api-key", apiKey.trim())
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException(parseGeminiError(response.code, bodyStr))
                }
                val root = JSONObject(bodyStr)
                val candidates = root.optJSONArray("candidates")
                val first = candidates?.optJSONObject(0)
                val parts = first?.optJSONObject("content")?.optJSONArray("parts")
                val sb = StringBuilder()
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        sb.append(parts.optJSONObject(i)?.optString("text").orEmpty())
                    }
                }
                val resultText = sb.toString().trim()
                if (resultText.isBlank()) {
                    throw IOException("Gemini trả về phản hồi trống. Hãy thử lại.")
                }
                resultText
            }
        }
    }

    private fun parseGeminiError(code: Int, rawBody: String): String {
        val serverMsg = runCatching {
            JSONObject(rawBody).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")
        return when (code) {
            400 -> "Yêu cầu không hợp lệ (400): ${serverMsg.ifBlank { "Kiểm tra lại định dạng tệp hoặc Model ID." }}"
            401, 403 -> "API Key không hợp lệ hoặc bị từ chối quyền ($code). Hãy kiểm tra lại khóa trong Cài đặt AI."
            404 -> "Không tìm thấy Model ID này ($code). Bấm 'Tải Model khả dụng' trong Cài đặt AI để chọn model khác."
            429 -> "Đã chạm giới hạn hạn mức (Quota 429). Hãy đợi vài giây hoặc chuyển sang model Flash khác trong Cài đặt AI."
            else -> "Lỗi kết nối Gemini ($code): ${serverMsg.ifBlank { "Kiểm tra kết nối mạng và thử lại." }}"
        }
    }
}
