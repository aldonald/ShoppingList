package com.example.shoppinglist

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView

class Login : AppCompatActivity() {
    lateinit var usernameField: EditText
    lateinit var passwordField: EditText
    lateinit var submitButton: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameField = findViewById(R.id.username)
        passwordField = findViewById(R.id.password)
        submitButton = findViewById(R.id.submit_button)

        submitButton.setOnClickListener {
            val usernameContent = usernameField.text.toString()
            val passwordContent = passwordField.text.toString()

            if (usernameContent == "admin" && passwordContent == "admin") {
                var builder = AlertDialog.Builder(this)
                builder.setIcon(R.drawable.ic_check_circle)
                builder.setTitle("Login Successful!")
                builder.setMessage("Welcome...")
                val alert = builder.create()
                alert.show()

                intent = Intent(this, MainActivity::class.java)
                this.startActivity(intent)

            } else {
                Toast.makeText(this, "Invalid login details!", Toast.LENGTH_SHORT).show()
            }

        }
    }
}
