package com.forex.chart.analysis.ai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.min

object ImageResizer {
    private const val MAX_SIZE = 1024 // টার্গেট সাইজ ১০২৪ পিক্সেল

    fun reduceImageSize(context: Context, imageUri: Uri): ByteArray? {
        return try {
            val contentResolver = context.contentResolver

            // ১. শুধু ডাইমেনশন চেক করা (মেমোরি সেভ করার জন্য)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            var inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // ২. স্যাম্পল সাইজ বের করা
            options.inSampleSize = calculateInSampleSize(options, MAX_SIZE, MAX_SIZE)
            options.inJustDecodeBounds = false

            // ৩. ইমেজ লোড করা
            inputStream = contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            originalBitmap?.let { bitmap ->
                // ৪. রিসাইজ করা
                val resizedBitmap = getResizedBitmap(bitmap, MAX_SIZE)

                // ৫. কম্প্রেস করে ByteArray বানানো (Quality 80%)
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