package com.forex.chart.analysis.ai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.min

object ImageResizer {
    private const val MAX_SIZE = 1024 // Target size: 1024px

    fun reduceImageSize(context: Context, imageUri: Uri): ByteArray? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds only to preserve memory
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // 2. Calculate optimal inSampleSize
            options.inSampleSize = calculateInSampleSize(options, MAX_SIZE, MAX_SIZE)
            options.inJustDecodeBounds = false

            // 3. Load subsampled bitmap
            inputStream = contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            originalBitmap?.let { bitmap ->
                // 4. Resize to target dimension
                val resizedBitmap = getResizedBitmap(bitmap, MAX_SIZE)

                // 5. Compress to ByteArray (80% JPEG quality)
                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getResizedBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val ratio = width.toFloat() / height.toFloat()

        val newWidth: Int
        val newHeight: Int

        if (ratio > 1) {
            // Landscape
            newWidth = min(width, maxSize)
            newHeight = (newWidth / ratio).toInt()
        } else {
            // Portrait
            newHeight = min(height, maxSize)
            newWidth = (newHeight * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}