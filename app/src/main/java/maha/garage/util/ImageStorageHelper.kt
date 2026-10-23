package maha.garage.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageHelper {

    suspend fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(sourceUri) ?: return@withContext null

            // 앱 내부 receipts 디렉토리
            val receiptsDir = File(context.filesDir, "receipts").apply {
                if (!exists()) mkdirs()
            }

            val filename = "receipt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val targetFile = File(receiptsDir, filename)

            // 비트맵 디코딩 및 용량 최적화 (가로세로 1600px 이내, JPEG 85% 압축)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap != null) {
                val maxDim = 1600
                val width = originalBitmap.width
                val height = originalBitmap.height
                val (scaledWidth, scaledHeight) = if (width > maxDim || height > maxDim) {
                    if (width > height) {
                        maxDim to (height * maxDim / width)
                    } else {
                        (width * maxDim / height) to maxDim
                    }
                } else {
                    width to height
                }

                val scaledBitmap = if (scaledWidth != width || scaledHeight != height) {
                    Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
                } else {
                    originalBitmap
                }

                FileOutputStream(targetFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    out.flush()
                }

                if (scaledBitmap != originalBitmap) {
                    scaledBitmap.recycle()
                }
                originalBitmap.recycle()

                targetFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 차량 대표 사진을 앱 내부 저장소(vehicle_images)에 영구 저장합니다.
     * SAF Content URI 임시 권한 만료로 인한 앱 재부팅 시 이미지 유실을 방지합니다.
     *
     * @return Room DB 저장을 위한 표준 로컬 파일 URI 문자열 (file:///...)
     */
    suspend fun saveVehicleImageToInternalStorage(context: Context, sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(sourceUri) ?: return@withContext null

            // 앱 내부 vehicle_images 디렉토리 (시스템 백업 대상 포함)
            val vehicleImagesDir = File(context.filesDir, "vehicle_images").apply {
                if (!exists()) mkdirs()
            }

            val filename = "vehicle_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val targetFile = File(vehicleImagesDir, filename)

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap != null) {
                val maxDim = 1920
                val width = originalBitmap.width
                val height = originalBitmap.height
                val (scaledWidth, scaledHeight) = if (width > maxDim || height > maxDim) {
                    if (width > height) {
                        maxDim to (height * maxDim / width)
                    } else {
                        (width * maxDim / height) to maxDim
                    }
                } else {
                    width to height
                }

                val scaledBitmap = if (scaledWidth != width || scaledHeight != height) {
                    Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)
                } else {
                    originalBitmap
                }

                FileOutputStream(targetFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    out.flush()
                }

                if (scaledBitmap != originalBitmap) {
                    scaledBitmap.recycle()
                }
                originalBitmap.recycle()

                // 표준 file:/// URI 반환
                Uri.fromFile(targetFile).toString()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Coil 이미지 로더가 항상 로컬 파일/캐시에서 안정적으로 로딩할 수 있도록 모델을 정규화합니다.
     */
    fun resolveImageModel(uriOrPath: String?): Any? {
        if (uriOrPath.isNullOrBlank()) return null
        return when {
            uriOrPath.startsWith("file://") -> Uri.parse(uriOrPath)
            uriOrPath.startsWith("content://") -> Uri.parse(uriOrPath)
            uriOrPath.startsWith("http://") || uriOrPath.startsWith("https://") -> uriOrPath
            else -> File(uriOrPath)
        }
    }

    /**
     * 이전 내부 저장소 사진을 안전하게 정리합니다.
     */
    fun deleteInternalPhotoIfPresent(context: Context, uriOrPath: String?) {
        if (uriOrPath.isNullOrBlank()) return
        try {
            val file = when {
                uriOrPath.startsWith("file://") -> File(Uri.parse(uriOrPath).path ?: return)
                !uriOrPath.startsWith("content://") && !uriOrPath.startsWith("http") -> File(uriOrPath)
                else -> return
            }
            if (file.exists() && file.canonicalPath.startsWith(context.filesDir.canonicalPath)) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
