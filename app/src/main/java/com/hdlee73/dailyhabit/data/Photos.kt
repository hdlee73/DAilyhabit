package com.hdlee73.dailyhabit.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/** 맛집 방문 사진. 앱 전용 폴더에 줄여서 저장한다 (폰 밖으로 나가지 않음) */
object Photos {
    private fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(context: Context, name: String): File? =
        name.takeIf { it.isNotBlank() }?.let { File(dir(context), it) }?.takeIf { it.exists() }

    fun delete(context: Context, name: String) {
        if (name.isNotBlank()) File(dir(context), name).delete()
    }

    /** 고른 사진을 긴 변 1280px 이하 JPEG로 줄여 저장하고 파일 이름을 돌려준다. 실패하면 null */
    suspend fun import(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val longest = max(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (longest / sample > 2560) sample *= 2
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null
            val rotation = resolver.openInputStream(uri)?.use {
                when (android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
            val scale = (1280f / max(decoded.width, decoded.height)).coerceAtMost(1f)
            val matrix = Matrix().apply {
                postScale(scale, scale)
                if (rotation != 0f) postRotate(rotation)
            }
            val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            val name = "p_${System.currentTimeMillis()}_${(0..9999).random()}.jpg"
            File(dir(context), name).outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            name
        }.getOrNull()
    }

    /** 화면에 그릴 작은 비트맵 */
    suspend fun load(context: Context, name: String, maxPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val f = file(context, name) ?: return@withContext null
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
            BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()
    }
}
