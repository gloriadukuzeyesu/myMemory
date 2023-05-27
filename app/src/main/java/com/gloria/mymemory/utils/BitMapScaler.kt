package com.gloria.mymemory.utils

import android.graphics.Bitmap

// an object bcz it doesn't make sense to have multiple object of BitMapScaler
object BitMapScaler {
    // Scale and maintain aspect ratio given a desired width
    // BitMapScaler.scaleToFitWidth(bitmap, 100)

    fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap {
        val factor: Float = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b, width, ((b.height * factor).toInt()), true)
    }

    // Scale and maintain  aspect ration given a desired height

    fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap {
        val factor: Float = height / b.height.toFloat()
        return Bitmap.createScaledBitmap(b, height, ((b.width * factor).toInt()), true)
    }

}
