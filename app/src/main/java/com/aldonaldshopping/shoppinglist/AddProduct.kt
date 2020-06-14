package com.aldonaldshopping.shoppinglist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.ByteArrayOutputStream


class AddProduct : AppCompatActivity() {
    private lateinit var newImageField: ImageView
    private lateinit var newNameField: EditText
    private lateinit var newPriceField: EditText
    private lateinit var submitButton: CardView
    private lateinit var queue: RequestQueue
    private lateinit var prefs: SharedPreferences
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_product)

        // Load Volley queue for web requests, prefs for local storage
        queue = Volley.newRequestQueue(this)
        prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        submitButton = findViewById(R.id.submitNewButton)
        newImageField = findViewById(R.id.newImageField)
        newNameField = findViewById(R.id.newNameField)
        newPriceField = findViewById(R.id.newPriceField)
        progressBar = findViewById(R.id.progress_product)

        // Set temporary image where a photo can be taken.
        newImageField.setOnClickListener {
            // Take the photo
            dispatchTakePictureIntent()
        }

        submitButton.setOnClickListener {
            // First we take the image that is stored in the image field
            val drawable = newImageField.drawable
            var imageBytes = ""
            // Now we convert the drawable into a bitmap and then a byte stream ready for sending.
            try {
                val bitmapDrawable = drawable as BitmapDrawable
                val bitmap = bitmapDrawable.bitmap
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)

                val imageInByte = stream.toByteArray()
                imageBytes = Base64.encodeToString(imageInByte, Base64.DEFAULT)
            } catch (e: ClassCastException) {
                // If the drawable could not be cast to BitmapDrawable this prevent a break. We do
                // not need to do anything as the variable has already been set as an empty string.
            }

            // Taking the text from the other entries
            val itemName = newNameField.text.toString()
            val itemPrice = newPriceField.text.toString()

            // Verify the price entered is reasonable. Two decimal places and max of 8 digits before
            // the point.
            val pricePattern = """^\d{0,8}(\.\d{0,2})?${'$'}""".toRegex()

            if (!pricePattern.matches(itemPrice)) {
                progressBar.visibility = View.GONE
                newPriceField.error = "The price entered is not correct."
            }

            // OK to add the spinner now price check has passed.
            progressBar.visibility = View.VISIBLE

            // Create a JSON object for the post request. This is the new item. The JSON request
            // needs to be built as per the specifications of the API. I am using vnd.api+json due
            // to it's strict and replicable structure
            val attributes = JSONObject()
            attributes.put("name", itemName)
            attributes.put("price", itemPrice)
            attributes.put("image", imageBytes)

            val data = JSONObject()
            data.put("attributes", attributes)
            data.put("type", "ShoppingItem")

            val params = JSONObject()
            params.put("data", data)

            // This is the url for posting new shopping items
            val url = "https://tranquil-lowlands-73758.herokuapp.com/api/add_shopping_item/"

            // The token is stored locally and needs to be added to the header for authorisation
            val token = prefs.getString("token", null)

            if (!token.isNullOrEmpty()) {
                // Create post request. It is created as an object so we can add headers required
                // for authorisation token and content type.
                val request = object: JsonObjectRequest(
                    Method.POST, url, params,
                    Response.Listener { _ ->
                        // When the post request is successful, an alert is created to advise the
                        // item was added and then the app is directed back to the main screen.
                        val builder = AlertDialog.Builder(this)
                        builder.setIcon(R.drawable.ic_thumb_up)
                        builder.setTitle("Item added!")
                        builder.setMessage("")
                        val alert = builder.create()
                        alert.show()

                        val intent = Intent(this, MainActivity::class.java)
                        progressBar.visibility = View.GONE
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
                // This adds the request built above to the request queue initialised in the
                // onCreate. The request is handled async.
                queue.add(request)
            }
        }
    }

    // This function handles the photo using the inbuilt camera. The camera also had to be turned
    // on in the manifest.
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, 1)
            }
        }
    }

    // This function sets the photo to the imageView when the photo has been taken.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            newImageField.setImageBitmap(imageBitmap)
        }
    }
}
