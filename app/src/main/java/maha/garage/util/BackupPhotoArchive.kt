package maha.garage.util

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Base64
import java.util.UUID

/** Portable references live only in the archive, never in the source database. */
object BackupPhotoArchive {
    private const val PREFIX = "maha-photo:"
    private const val MAX_PHOTO = 12 * 1024 * 1024
    private const val MAX_TOTAL = 96 * 1024 * 1024
    private fun references(root: JSONObject): List<Pair<JSONObject, String>> = buildList {
        for ((array, field) in listOf("cars" to "photoUri", "serviceVisits" to "receiptPhotoUri", "carInspections" to "certificatePhotoUri")) {
            val values = root.optJSONArray(array) ?: continue
            for (i in 0 until values.length()) add(values.getJSONObject(i) to field)
        }
    }
    fun pack(context: Context, root: JSONObject) {
        val archive = JSONArray()
        val ids = mutableMapOf<String, String>()
        var total = 0
        var missing = 0
        for ((record, field) in references(root)) {
            val path = record.optString(field)
            if (path.isBlank()) continue
            val existing = ids[path]
            if (existing != null) { record.put(field, PREFIX + existing); continue }
            val bytes = try {
                val uri = Uri.parse(path)
                val input = if (uri.scheme == "content") context.contentResolver.openInputStream(uri)
                    else File(if (uri.scheme == "file") requireNotNull(uri.path) else path).inputStream()
                requireNotNull(input).use { maha.garage.domain.engine.TransferPayloadCodec.readLimited(it, MAX_PHOTO) }
            } catch (e: IllegalArgumentException) { throw e }
              catch (e: Exception) { null }
            if (bytes == null) { missing++; continue }
            require(bytes.size <= MAX_PHOTO) { "사진 1개가 12MB를 초과합니다. 사진을 줄인 뒤 다시 백업해 주세요." }
            total += bytes.size
            require(total <= MAX_TOTAL) { "사진 합계가 96MB를 초과합니다. 사진을 나누어 백업해 주세요." }
            val id = UUID.randomUUID().toString()
            ids[path] = id
            archive.put(JSONObject().put("id", id).put("data", Base64.getEncoder().encodeToString(bytes)))
            record.put(field, PREFIX + id)
        }
        root.put("attachments", archive)
        root.put("missingPhotos", missing)
    }
    fun unpack(context: Context, root: JSONObject, created: MutableList<File>): Int {
        val archive = root.optJSONArray("attachments") ?: JSONArray()
        val files = mutableMapOf<String, File>()
        var total = 0
        val dir = File(context.filesDir, "restored_photos").apply { check(exists() || mkdirs()) }
        for (i in 0 until archive.length()) {
            val entry = archive.getJSONObject(i)
            val id = entry.getString("id")
            require(id !in files) { "중복된 사진 정보입니다." }
            val encoded = entry.getString("data")
            require(encoded.length <= (MAX_PHOTO.toLong() * 4 / 3 + 8)) { "사진 파일이 너무 큽니다." }
            val bytes = Base64.getDecoder().decode(encoded)
            total += bytes.size
            require(bytes.size <= MAX_PHOTO && total <= MAX_TOTAL) { "사진 백업 크기를 초과했습니다." }
            val file = File(dir, UUID.randomUUID().toString() + ".jpg")
            created.add(file)
            file.writeBytes(bytes)
            files[id] = file
        }
        var missing = 0
        for ((record, field) in references(root)) {
            val path = record.optString(field)
            if (path.startsWith(PREFIX)) {
                val file = requireNotNull(files[path.removePrefix(PREFIX)]) { "백업에 사진 파일이 빠져 있습니다." }
                record.put(field, file.absolutePath)
            } else if (path.isNotBlank()) {
                // Legacy paths may still work on the same phone; never retain a broken foreign path.
                val exists = runCatching {
                    val uri = Uri.parse(path)
                    if (uri.scheme == "content") context.contentResolver.openInputStream(uri)?.use { true } == true
                    else File(if (uri.scheme == "file") requireNotNull(uri.path) else path).isFile
                }.getOrDefault(false)
                if (!exists) { missing++; record.put(field, "") }
            }
        }
        return maxOf(missing, root.optInt("missingPhotos", 0))
    }
}
