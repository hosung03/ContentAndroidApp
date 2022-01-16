package com.visang.mathalive.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.opengl.ETC1.encodeImage
import android.util.Base64
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL


object ImageUtils {

    fun getCroppedBitmap(bitmap: Bitmap, left: Int, top: Int, width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    fun getCroppedBitmap(bitmap: Bitmap, rect: Rect): Bitmap {
        return Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
    }

    fun imageToJpegBytes(bitmap: Bitmap): ByteArray {
        val byteOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, Constants.JPEG_QUALITY, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    fun save(bitmap: Bitmap): File {
        val bytes = imageToJpegBytes(bitmap)
        return Utils.save(bytes, PathUtils.getOutputFilePath("jpg"))
    }

    fun imageToBase64(imageUrl: String): String {
        val url = URL(imageUrl)
        val bis = BufferedInputStream(url.openConnection().getInputStream())

        val bm = BitmapFactory.decodeStream(bis)
        val b = imageToJpegBytes(bm)
        val encImage: String = Base64.encodeToString(b, Base64.DEFAULT)

        return encImage
    }

}
