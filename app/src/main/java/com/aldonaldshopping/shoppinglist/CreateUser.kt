package com.aldonaldshopping.shoppinglist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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


class CreateUser : AppCompatActivity() {
    private lateinit var username: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var verifyPassword: EditText
    private lateinit var createUserButton: CardView
    private lateinit var prefs: SharedPreferences
    private lateinit var idPrefs: SharedPreferences
    private lateinit var queue: RequestQueue
    private lateinit var progressBar: ProgressBar
    private val addUserUrl = "https://tranquil-lowlands-73758.herokuapp.com/api/create_user/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_user)

        // Local storage - accessing the token and also the consumer's id
        prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        idPrefs = getSharedPreferences("id", Context.MODE_PRIVATE)

        // Volley queue for internet requests
        queue = Volley.newRequestQueue(this)

        username = findViewById(R.id.create_username)
        email = findViewById(R.id.createEmail)
        password = findViewById(R.id.createPassword)
        verifyPassword = findViewById(R.id.createPasswordVerify)
        createUserButton = findViewById(R.id.createUserButton)
        progressBar = findViewById(R.id.progress_create)

        // This pattern is used to verify emails
        val emailPattern = "^([a-zA-Z0-9_\\-\\.\\+]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)\$".toRegex()

        createUserButton.setOnClickListener {
            val usernameText = username.text.toString()
            val emailText = email.text.toString()
            val passwordText = password.text.toString()
            val verifyText = verifyPassword.text.toString()
            // Make sure the two password fields match
            if (passwordText != verifyText) {
                Toast.makeText(this, "Password fields do not match", Toast.LENGTH_SHORT).show()
            } else {
                // Check the email complies
                if (!emailPattern.matches(emailText)) {
                    email.error = "The email address is incorrect."
                } else if (passwordText.length < 8) {
                    // Prevent short passwords
                    password.error = "The password is too short. Please enter at least 8 characters."
                } else {
                    // Build the JSON blob to post the new user details
                    val attributes = JSONObject()
                    attributes.put("username", usernameText)
                    attributes.put("email", emailText)
                    attributes.put("password", passwordText)

                    val data = JSONObject()
                    data.put("attributes", attributes)
                    data.put("type", "User")

                    val params = JSONObject()
                    params.put("data", data)

                    // The request is an object so that the header can be added to specify content
                    // type. application/vnd.api+json is more structured and scalable than
                    // application/json.
                    val request = object : JsonObjectRequest(
                        Method.POST, addUserUrl, params,
                        Response.Listener { response ->
                            val jsonObject = response.getJSONObject("data")

                            val username =
                                jsonObject.getJSONObject("attributes").getString("username")

                            // When the user has been created, there are several steps to now log
                            // in. Therefore we now get a spinner to advide the user the app is
                            // loading. There are several steps to allow the use of tokens to log in
                            // - this prevents the app needing to send plain text usernames and
                            // passwords in post requests.
                            progressBar.visibility = View.VISIBLE

                            // Call my login function below.
                            login(username, passwordText)
                        },
                        Response.ErrorListener {
                            it.printStackTrace()
                        }
                    ) {
                        override fun getHeaders(): Map<String, String> {
                            val header = HashMap<String, String>()
                            header["Content-Type"] = "application/vnd.api+json"
                            return header
                        }
                    }
                    // Add request to the volley queue. This allows it to be requested async.
                    queue.add(request)
                }
            }
        }

    }

    private fun login(username: String, password: String) {
        // New url to send login details so the app receives a token for future requests.
        val url = "https://tranquil-lowlands-73758.herokuapp.com/api-token-auth/"

        val params = JSONObject()
        params.put("username", username)
        params.put("password", password)

        // This section is built like this as the volley requests are async - therefore the next
        // request is only built on the completion of the last request. The outside request does not
        // need to be an object as no headers are required. The end point does not need a token as
        // this is to get a token and the endpoint accepts vanilla JSON.
        val request = JsonObjectRequest(
            Request.Method.POST, url, params,
            Response.Listener<JSONObject> { response ->
                try {
                    // The response is just a token which is stored to local storage and sent with
                    // all future requests to authorise.
                    val token = response.getString("token")
                    with (prefs.edit()) {
                        putString("token", token)
                        commit()
                    }

                    // This request is to get the id for the customer. A filter was added to the
                    // endpoint to allow searching via the API.
                    val getId = object: JsonObjectRequest(
                        Method.GET, "https://tranquil-lowlands-73758.herokuapp.com/api/accounts/?filter[username]=$username", null,
                        Response.Listener {idResponse ->
                            try {
                                // As the username is unique, there will only be a single response
                                // in the list. The id is stored to local storage. This id will be
                                // used to link up the Pusher notification requests.
                                val userArray = idResponse.getJSONArray("data")
                                val userObject = userArray.getJSONObject(0)
                                val id = userObject.getString("id")
                                with(prefs.edit()) {
                                    putString("id", id)
                                    commit()
                                }
                                // Now the id has been received we can log the device with Beams -
                                // a manager for FireBase messaging. Below is essentially a post
                                // request which is packaged and sent to beams. When they want to
                                // match the users, they initiate the call below as is.
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

                                    // The following sets the user up in Beams.
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

                                // Now we can give the log in message. Failures on the Beams set up
                                // are logged but silent to the user as they do not prevent the use
                                // of the app.
                                val builder = AlertDialog.Builder(this)
                                builder.setIcon(R.drawable.ic_check_circle)
                                builder.setTitle("Login Successful!")
                                builder.setMessage("Welcome...")
                                val alert = builder.create()
                                alert.show()

                                // Navigate to the MainActivity
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
                    // This is the request for user id added to the queue.
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
        // This is the request to get the token added to the queue.
        queue.add(request)
    }
}
