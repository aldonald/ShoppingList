package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.pusher.pushnotifications.BeamsCallback
import com.pusher.pushnotifications.PushNotifications
import com.pusher.pushnotifications.PusherCallbackError
import com.pusher.pushnotifications.auth.AuthData
import com.pusher.pushnotifications.auth.AuthDataGetter
import com.pusher.pushnotifications.auth.BeamsTokenProvider
import org.json.JSONException
import org.json.JSONObject

class Login : AppCompatActivity() {
    lateinit var usernameField: EditText
    lateinit var passwordField: EditText
    lateinit var submitButton: CardView
    private lateinit var prefs: SharedPreferences
    private lateinit var queue: RequestQueue
    private lateinit var signUpButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        queue = Volley.newRequestQueue(this)
        usernameField = findViewById(R.id.username)
        passwordField = findViewById(R.id.password)
        submitButton = findViewById(R.id.submit_button)
        signUpButton = findViewById(R.id.sign_up_button)

        submitButton.setOnClickListener {
            val usernameContent = usernameField.text.toString()
            val passwordContent = passwordField.text.toString()

            login(usernameContent, passwordContent)
        }

        signUpButton.setOnClickListener {
            val intent = Intent(this, CreateUser::class.java)
            this.startActivity(intent)
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
                    val getId = object: JsonObjectRequest(
                        Method.GET, "https://tranquil-lowlands-73758.herokuapp.com/api/accounts/?filter[username]=$username", null,
                        Response.Listener {idResponse ->
                            try {
                                val userArray = idResponse.getJSONArray("data")
                                val userObject = userArray.getJSONObject(0)
                                val id = userObject.getString("id")
                                with(prefs.edit()) {
                                    putString("id", id)
                                    commit()
                                }

                                try {
                                    val tokenProvider = BeamsTokenProvider(
                                        "https://tranquil-lowlands-73758.herokuapp.com/api/accounts/beams_auth/",
                                        object : AuthDataGetter {
                                            override fun getAuthData(): AuthData {
                                                return AuthData(
                                                    headers = hashMapOf(
                                                        Pair("Authorization", "Token $token"),
                                                        Pair(
                                                            "Content-Type",
                                                            "application/vnd.api+json"
                                                        )
                                                    ),
                                                    queryParams = hashMapOf()
                                                )
                                            }
                                        }
                                    )

                                    PushNotifications.setUserId(
                                        id,
                                        tokenProvider,
                                        object : BeamsCallback<Void, PusherCallbackError> {
                                            override fun onFailure(error: PusherCallbackError) {
                                                Log.e(
                                                    "BeamsAuth",
                                                    "Could not login to Beams: ${error.message}"
                                                );
                                            }

                                            override fun onSuccess(vararg values: Void) {
                                                Log.i("BeamsAuth", "Beams login success")
                                                Log.i("BeamsAuth", "$values")
                                            }
                                        }
                                    )
                                } catch (e: IllegalStateException) {
                                    e.printStackTrace()
                                }

                                val builder = AlertDialog.Builder(this)
                                builder.setIcon(R.drawable.ic_check_circle)
                                builder.setTitle("Login Successful!")
                                builder.setMessage("Welcome...")
                                val alert = builder.create()
                                alert.show()

                                val intent = Intent(this, MainActivity::class.java)
                                this.startActivity(intent)

                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        },
                        Response.ErrorListener {
                            Toast.makeText(this, "Unable to connect to server!", Toast.LENGTH_SHORT).show()
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
                    queue.add(getId)
                } catch (e: JSONException) {
                    Toast.makeText(this, "Invalid login details!", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
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
