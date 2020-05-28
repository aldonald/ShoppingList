package com.example.shoppinglist

import android.graphics.Bitmap
import android.media.Image
import java.io.Serializable

data class ShoppingItem(val id: String, val name: String, val imageUrl: String, val price: String) : Serializable