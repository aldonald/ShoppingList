package com.example.shoppinglist

import android.app.DownloadManager
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

// The Activity implements AppCompatActivity() class but also the OnItemSelectListener interface
// which links the recyclerView click event to this activity.
class MainActivity : AppCompatActivity(), RecyclerAdapter.OnItemSelectListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecyclerAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var result: String
    private var shoppingList = ArrayList<ShoppingItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        recyclerView = findViewById(R.id.recyclerView)

        parseJson()

        // Below finds the recyclerView from the layout and attaches the adapter and passes the
        // OnItemSelectListener which is used by this Activity as an interface in order to link the
        // click event to the parent activity for the fragment.
        recyclerView = findViewById(R.id.recyclerView)
        adapter = RecyclerAdapter(shoppingList, this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        // This reduces the resources required and can be done as the recyclerView content will be
        // unchanged.
        recyclerView.setHasFixedSize(false)
    }

    private fun parseJson() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://tranquil-lowlands-73758.herokuapp.com/api/shoppingitems/"

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            Response.Listener<JSONObject> { response ->
                // Display the first 500 characters of the response string.
                var updatedList = ArrayList<ShoppingItem>()
                try {
                    val jsonArray = response.getJSONArray("data")
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        updatedList.add(ShoppingItem(
                            id = jsonObject.getInt("id"),
                            name = jsonObject.getJSONObject("attributes").getString("name"),
                            price = jsonObject.getJSONObject("attributes").getString("price"),
                            barcode = jsonObject.getJSONObject("attributes").getString("barcode")
                        ))
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                } finally {
                    adapter.updateShoppingList(updatedList)
                }
            },
            Response.ErrorListener {
                it.printStackTrace()
            }
        )

        queue.add(request)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu to use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle selection of options in the title bar
        when (item.itemId) {
            R.id.choose_shop -> {
                Toast.makeText(this, "Choose shop was selected", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.log_out -> {
                Toast.makeText(this, "Logout was selected", Toast.LENGTH_SHORT).show()
                intent = Intent(this, Login::class.java)
                this.startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun run() {
        val url = "https://tranquil-lowlands-73758.herokuapp.com/api/shoppingitems/"

        with(URL(url).openConnection() as HttpURLConnection) {
            // optional default is GET
            requestMethod = "POST"

            val wr = OutputStreamWriter(outputStream);
            //wr.write(reqParam);
            wr.flush();

            println("URL : $url")
            println("Response Code : $responseCode")

            BufferedReader(InputStreamReader(inputStream)).use {
                val response = StringBuffer()

                var inputLine = it.readLine()
                while (inputLine != null) {
                    response.append(inputLine)
                    inputLine = it.readLine()
                }
                println("Response : $response")
            }
        }
//        internetClient.newCall(request).execute().use { response ->
//            if (!response.isSuccessful) throw IOException("Unexpected code $response")
//
//            for ((name, value) in response.headers) {
//                println("$name: $value")
//            }
//
//            println(response.body!!.string())
//        }
    }

    // This is the main function to take the item selected and create a new fragment with the
    // selected item. This has been given a tag to be able to be identified later although this is
    // not required in this code.
    override fun onItemSelected(position: Int) {
        val newsSource = shoppingList[position]
        val fragment = ShoppingItemFragment.newInstance(newsSource)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.main_content, fragment, "SHOPPING_ITEM_FRAGMENT")
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
