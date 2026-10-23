package maha.garage.domain.engine

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

data class EncodedTransfer(val parts: List<String>, val checksum: String)
object TransferPayloadCodec {
    const val PART_SIZE = 160_000
    const val MAX_RAW = 160 * 1024 * 1024
    const val MAX_PACKED = 64 * 1024 * 1024
    fun checksum(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    fun readLimited(input: InputStream, max: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            require(out.size().toLong() + n <= max) { "백업 크기 제한을 초과했습니다." }
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
    fun encode(json: String): EncodedTransfer {
        val raw = json.toByteArray(Charsets.UTF_8)
        require(raw.size <= MAX_RAW) { "백업이 너무 큽니다. 파일 백업을 이용해 주세요." }
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(raw) }
        require(out.size() <= MAX_PACKED) { "전송 데이터가 너무 큽니다. 파일 백업을 이용해 주세요." }
        return EncodedTransfer(Base64.getEncoder().encodeToString(out.toByteArray()).chunked(PART_SIZE), checksum(raw))
    }
    fun decode(parts: List<String>, expectedChecksum: String): String {
        require(parts.isNotEmpty() && parts.all { it.isNotEmpty() && it.length <= PART_SIZE }) { "전송 조각이 올바르지 않습니다." }
        require(parts.sumOf { it.length.toLong() } <= MAX_PACKED.toLong() * 4 / 3 + 8) { "전송 데이터가 너무 큽니다." }
        val packed = Base64.getDecoder().decode(parts.joinToString(""))
        val raw = GZIPInputStream(ByteArrayInputStream(packed)).use { readLimited(it, MAX_RAW) }
        require(checksum(raw) == expectedChecksum) { "데이터 검증에 실패했습니다. 다시 전송해 주세요." }
        return raw.toString(Charsets.UTF_8)
    }
}
