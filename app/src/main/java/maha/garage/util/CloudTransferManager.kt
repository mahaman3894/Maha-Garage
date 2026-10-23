package maha.garage.util

import android.content.Context
import android.os.Build
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import maha.garage.data.local.MahaGarageDatabase
import maha.garage.domain.engine.TransferPayloadCodec
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit

data class CloudTransferInfo(val transferKey: String, val carsCount: Int, val visitsCount: Int, val createdAt: Long, val expiresAt: Long, val missingPhotos: Int = 0)

object CloudTransferManager {
    private const val COLLECTION_NAME = "device_transfers"
    private const val EXPIRE_DURATION_MILLIS = 48L * 60 * 60 * 1000
    private fun cleanup(db: FirebaseFirestore, ids: List<String>) {
        ids.chunked(100).forEach { group -> runCatching {
            val batch = db.batch()
            group.forEach { batch.delete(db.collection(COLLECTION_NAME).document(it)) }
            Tasks.await(batch.commit(), 30, TimeUnit.SECONDS)
        } }
    }
    suspend fun uploadTransferData(context: Context, db: MahaGarageDatabase): Result<CloudTransferInfo> = withContext(Dispatchers.IO) {
        val firestore = FirebaseFirestore.getInstance()
        val owned = mutableListOf<String>()
        try {
            val root = BackupRestoreManager.generateBackupJson(db, context)
            val cars = root.optJSONArray("cars")?.length() ?: 0
            val visits = root.optJSONArray("serviceVisits")?.length() ?: 0
            require(cars > 0) { "등록된 차량 데이터가 없습니다." }
            val encoded = TransferPayloadCodec.encode(root.toString())
            val now = System.currentTimeMillis()
            val expires = now + EXPIRE_DURATION_MILLIS
            val parts = encoded.parts.indices.map { "part_${UUID.randomUUID()}_$it" }
            val base = hashMapOf<String, Any>("formatVersion" to 2, "kind" to "manifest", "status" to "uploading",
                "createdAt" to now, "expiresAt" to expires, "expiresAtTimestamp" to com.google.firebase.Timestamp(java.util.Date(expires)), "carsCount" to cars, "visitsCount" to visits,
                "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}", "isUsed" to false,
                "chunkIds" to parts, "checksum" to encoded.checksum)
            var key: String? = null
            repeat(20) {
                if (key == null) {
                    val candidate = "%06d".format(java.util.Locale.US, SecureRandom().nextInt(1_000_000))
                    val ref = firestore.collection(COLLECTION_NAME).document(candidate)
                    val reserved = Tasks.await(firestore.runTransaction { tx ->
                        if (tx.get(ref).exists()) false else { tx.set(ref, base + ("transferKey" to candidate)); true }
                    }, 60, TimeUnit.SECONDS)
                    if (reserved) { key = candidate; owned.add(candidate) }
                }
            }
            val actualKey = requireNotNull(key) { "전송 번호 발급에 실패했습니다. 다시 시도해 주세요." }
            // Each document is well below 1MiB; each batch stays below the request size limit.
            parts.indices.toList().chunked(32).forEach { indices ->
                val batch = firestore.batch()
                indices.forEach { i ->
                    owned.add(parts[i])
                    batch.set(firestore.collection(COLLECTION_NAME).document(parts[i]), mapOf(
                        "kind" to "chunk", "transferKey" to actualKey, "createdAt" to now, "expiresAt" to expires,
                        "expiresAtTimestamp" to com.google.firebase.Timestamp(java.util.Date(expires)), "index" to i, "data" to encoded.parts[i]))
                }
                Tasks.await(batch.commit(), 90, TimeUnit.SECONDS)
                ensureActive()
            }
            Tasks.await(firestore.collection(COLLECTION_NAME).document(actualKey).update("status", "ready"), 30, TimeUnit.SECONDS)
            Result.success(CloudTransferInfo(actualKey, cars, visits, now, expires, root.optInt("missingPhotos", 0)))
        } catch (e: CancellationException) { cleanup(firestore, owned); throw e }
        catch (e: Exception) { cleanup(firestore, owned); Result.failure(e) }
    }

    suspend fun downloadAndRestore(context: Context, db: MahaGarageDatabase, inputKey: String): Result<ImportResult> = withContext(Dispatchers.IO) {
        val key = inputKey.trim().replace("-", "").replace(" ", "")
        if (!key.matches(Regex("[0-9]{6}"))) return@withContext Result.failure(IllegalArgumentException("6자리 숫자 전송 번호를 입력해 주세요."))
        val firestore = FirebaseFirestore.getInstance()
        val ref = firestore.collection(COLLECTION_NAME).document(key)
        val token = UUID.randomUUID().toString()
        var claimed = false
        var restored = false
        try {
            // Reserve restoration so two phones cannot consume the same transfer concurrently.
            val data = Tasks.await(firestore.runTransaction { tx ->
                val snapshot = tx.get(ref)
                require(snapshot.exists()) { "전송 번호를 찾을 수 없습니다." }
                val now = System.currentTimeMillis()
                require((snapshot.getLong("expiresAt") ?: 0) >= now) { "전송 번호가 만료되었습니다. 이전 폰에서 다시 발급해 주세요." }
                require(snapshot.getBoolean("isUsed") != true) { "이미 사용한 전송 번호입니다. 이전 폰에서 새 번호를 발급해 주세요." }
                val status = snapshot.getString("status")
                require(status != "uploading") { "이전 폰에서 아직 업로드 중입니다." }
                require(status != "restoring" || (snapshot.getLong("claimExpiresAt") ?: 0) < now) { "다른 폰에서 복원 중입니다." }
                tx.update(ref, mapOf("status" to "restoring", "claimToken" to token, "claimExpiresAt" to now + 30 * 60 * 1000))
                snapshot.data.orEmpty()
            }, 60, TimeUnit.SECONDS)
            claimed = true
            val ids = (data["chunkIds"] as? List<*>)?.map { requireNotNull(it as? String) } ?: emptyList()
            val payload = if ((data["formatVersion"] as? Number)?.toInt() == 2) {
                require(ids.isNotEmpty() && ids.size <= 600 && ids.distinct().size == ids.size && ids.all { it.startsWith("part_") && !it.contains('/') }) { "전송 목록이 손상되었습니다." }
                val parts = ids.withIndex().toList().chunked(8).flatMap { group ->
                    coroutineScope { group.map { (index, id) -> async {
                        val piece = Tasks.await(firestore.collection(COLLECTION_NAME).document(id).get(), 60, TimeUnit.SECONDS)
                        require(piece.getString("transferKey") == key && piece.getLong("index") == index.toLong()) { "전송 조각이 일치하지 않습니다." }
                        requireNotNull(piece.getString("data")) { "전송 조각이 빠져 있습니다." }
                    } }.awaitAll() }
                }
                TransferPayloadCodec.decode(parts, requireNotNull(data["checksum"] as? String))
            } else requireNotNull(data["payload"] as? String) { "전송 내용이 비어 있습니다." } // Older apps' backups remain readable.
            val result = BackupRestoreManager.importBackup(context, db, payload)
            if (result.isSuccess) {
                restored = true
                // Preserve successful local restoration even if the final cloud acknowledgement is temporarily offline.
                runCatching { Tasks.await(ref.update(mapOf("isUsed" to true, "status" to "used")), 30, TimeUnit.SECONDS) }
                cleanup(firestore, ids)
            }
            result
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
        finally {
            if (claimed && !restored) runCatching {
                Tasks.await(firestore.runTransaction { tx ->
                    if (tx.get(ref).getString("claimToken") == token) tx.update(ref, mapOf("status" to "ready", "claimToken" to "", "claimExpiresAt" to 0L))
                    true
                }, 30, TimeUnit.SECONDS)
            }
        }
    }
}
