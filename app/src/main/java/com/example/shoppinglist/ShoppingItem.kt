package com.example.shoppinglist

import java.io.Serializable

data class ShoppingItem(val id: Int, val name: String, val price: String, val barcode: String) : Serializable