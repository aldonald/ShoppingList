package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.HttpResponse
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream


class AddProduct : AppCompatActivity() {
    private lateinit var newImageField: ImageView
    private lateinit var newNameField: EditText
    private lateinit var newPriceField: EditText
    private lateinit var submitButton: Button
    private lateinit var queue: RequestQueue
    private lateinit var prefs: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_product)

        queue = Volley.newRequestQueue(this)
        prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        submitButton = findViewById(R.id.submitNewButton)
        newImageField = findViewById(R.id.newImageField)
        newNameField = findViewById(R.id.newNameField)
        newPriceField = findViewById(R.id.newPriceField)

        newImageField.setOnClickListener {
            dispatchTakePictureIntent()
        }

        submitButton.setOnClickListener {
            // Getting the byte stream code is heavily influenced by
            // https://stackoverflow.com/questions/9042932/getting-image-from-imageview
            val drawable = newImageField.drawable
            var imageBytes = ""
            try {
                val bitmapDrawable = drawable as BitmapDrawable
                val bitmap = bitmapDrawable.bitmap
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)

                val imageInByte = stream.toByteArray()
                imageBytes = Base64.encodeToString(imageInByte, Base64.DEFAULT)
            } catch (e: ClassCastException) {
            }

            val itemName = newNameField.text.toString()
            val itemPrice = newPriceField.text.toString()

            val attributes = JSONObject()
            attributes.put("name", itemName)
            attributes.put("price", itemPrice)
            attributes.put("image", imageBytes)

            val data = JSONObject()
            data.put("attributes", attributes)
            data.put("type", "ShoppingItem")

            val params = JSONObject()
            params.put("data", data)
            val url = "https://tranquil-lowlands-73758.herokuapp.com/api/add_shopping_item/"
            val token = prefs.getString("token", null)

            if (!token.isNullOrEmpty()) {
                val request = object: JsonObjectRequest(
                    Method.POST, url, params,
                    Response.Listener { _ ->
                        val builder = AlertDialog.Builder(this)
                        builder.setIcon(R.drawable.ic_thumb_up)
                        builder.setTitle("Item added!")
                        builder.setMessage("")
                        val alert = builder.create()
                        alert.show()

                        val intent = Intent(this, MainActivity::class.java)
                        this.startActivity(intent)
                    },
                    Response.ErrorListener {
                        it.printStackTrace()
                    }
                ) {
                    override fun getHeaders() : Map<String,String> {
                        val header = HashMap<String, String>()
                        header["Authorization"] = "Token $token"
                        header["Content-Type"] = "application/vnd.api+json"
                        return header
                    }
                }
                queue.add(request)
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, 1)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            newImageField.setImageBitmap(imageBitmap)
        }
    }
}
