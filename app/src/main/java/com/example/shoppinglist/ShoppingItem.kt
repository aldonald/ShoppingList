package com.example.shoppinglist

import java.io.Serializable

data class ShoppingItem(val id: String, val name: String, val price: String) : Serializable