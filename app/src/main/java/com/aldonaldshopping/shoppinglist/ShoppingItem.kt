package com.aldonaldshopping.shoppinglist

import java.io.Serializable

// Data class for items. The image is a url string so can be fetched when required.
data class ShoppingItem(val id: String, val name: String, val imageUrl: String, val price: String) : Serializable