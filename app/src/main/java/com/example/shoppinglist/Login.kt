package com.example.shoppinglist

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class Login : AppCompatActivity() {
    lateinit var usernameField: EditText
    lateinit var passwordField: EditText
    lateinit var submitButton: CardView
    private lateinit var prefs: SharedPreferences
    private lateinit var queue: RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        queue = Volley.newRequestQueue(this)

        usernameField = findViewById(R.id.username)
        passwordField = findViewById(R.id.password)
        submitButton = findViewById(R.id.submit_button)

        submitButton.setOnClickListener {
            val usernameContent = usernameField.text.toString()
            val passwordContent = passwordField.text.toString()

            val url = "https://tranquil-lowlands-73758.herokuapp.com/api-token-auth/"
            val params = JSONObject()
            params.put("username", usernameContent)
            params.put("password", passwordContent)

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

                        intent = Intent(this, MainActivity::class.java)
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
}
