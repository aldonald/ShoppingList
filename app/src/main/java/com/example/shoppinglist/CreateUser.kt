package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class CreateUser : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var verifyPassword: EditText
    private lateinit var createUserButton: Button
    private lateinit var prefs: SharedPreferences
    private lateinit var queue: RequestQueue
    private val addUserUrl = "https://tranquil-lowlands-73758.herokuapp.com/api/create_user/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_user)

        prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        queue = Volley.newRequestQueue(this)
        username = findViewById(R.id.create_username)
        email = findViewById(R.id.createEmail)
        password = findViewById(R.id.createPassword)
        verifyPassword = findViewById(R.id.createPasswordVerify)
        createUserButton = findViewById(R.id.createUserButton)

        createUserButton.setOnClickListener {
            val usernameText = username.text.toString()
            val emailText = email.text.toString()
            val passwordText = password.text.toString()
            val verifyText = verifyPassword.text.toString()
            if (passwordText != verifyText) {
                Toast.makeText(this, "Password fields do not match", Toast.LENGTH_SHORT).show()
            } else {
                val attributes = JSONObject()
                attributes.put("username", usernameText)
                attributes.put("email", emailText)
                attributes.put("password", passwordText)

                val data = JSONObject()
                data.put("attributes", attributes)
                data.put("type", "User")

                val params = JSONObject()
                params.put("data", data)

                val request = object: JsonObjectRequest(
                    Method.POST, addUserUrl, params,
                    Response.Listener { response ->
                        val jsonObject = response.getJSONObject("data")
                        val username = jsonObject.getJSONObject("attributes").getString("username")

                        login(username, passwordText)
                    },
                    Response.ErrorListener {
                        it.printStackTrace()
                    }
                ) {
                    override fun getHeaders() : Map<String,String> {
                        val header = HashMap<String, String>()
                        header["Content-Type"] = "application/vnd.api+json"
                        return header
                    }
                }
                queue.add(request)
            }
        }
    }

    private fun login(username: String, password: String) {
        val url = "https://tranquil-lowlands-73758.herokuapp.com/api-token-auth/"
        val params = JSONObject()
        params.put("username", username)
        params.put("password", password)

        val request = JsonObjectRequest(
            Request.Method.POST, url, params,
            Response.Listener<JSONObject> { response ->
                try {
                    val token = response.getString("token")
                    with (prefs.edit()) {
                        putString("token", token)
                        commit()
                    }

                } catch (e: JSONException) {
                    Toast.makeText(this, "Invalid login details!", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                } finally {
                    val builder = AlertDialog.Builder(this)
                    builder.setIcon(R.drawable.ic_check_circle)
                    builder.setTitle("Login Successful!")
                    builder.setMessage("Welcome...")
                    val alert = builder.create()
                    alert.show()

                    val intent = Intent(this, MainActivity::class.java)
                    this.startActivity(intent)
                }
            },
            Response.ErrorListener {
                Toast.makeText(this, "Unable to connect to server!", Toast.LENGTH_SHORT).show()
                it.printStackTrace()
            }
        )
        queue.add(request)
    }
}
