package com.lumi.meds

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

const val MAX_SIDE = 1600
private const val JPEG_QUALITY = 80

/** Arquivo temporário onde a câmera grava a foto antes de salvar. */
fun photoFile(context: Context) = File(context.cacheDir, "receita.jpg")

fun photoUri(context: Context): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile(context))

/** Maior potência de 2 que deixa o lado maior ainda >= maxSide (decodifica sem estourar memória). */
fun sampleSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Int {
    var s = 1
    while (maxOf(width, height) / (s * 2) >= maxSide) s *= 2
    return s
}

/** Foto da câmera -> JPEG com no máximo [MAX_SIDE]px no lado maior, já na orientação certa. */
fun compressPhoto(file: File): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }
    var bmp = BitmapFactory.decodeFile(file.path, opts) ?: error("Foto inválida")

    val scale = MAX_SIDE.toFloat() / maxOf(bmp.width, bmp.height)
    val rotation = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (scale < 1f || rotation != 0f) {
        val m = Matrix().apply { if (scale < 1f) postScale(scale, scale); postRotate(rotation) }
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }
    return ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }.toByteArray()
}
