package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pusher.pushnotifications.BeamsCallback
import com.pusher.pushnotifications.PushNotifications
import com.pusher.pushnotifications.PusherCallbackError
import com.pusher.pushnotifications.auth.AuthData
import com.pusher.pushnotifications.auth.AuthDataGetter
import com.pusher.pushnotifications.auth.BeamsTokenProvider
import org.json.JSONException
import org.json.JSONObject


// The Activity implements AppCompatActivity() class but also the OnItemSelectListener interface
// which links the recyclerView click event to this activity.
class MainActivity : AppCompatActivity(), RecyclerAdapter.OnItemSelectListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecyclerAdapter
    private lateinit var fab: FloatingActionButton
    private lateinit var toolbar: Toolbar
    private var shoppingList = ArrayList<ShoppingItem>()
    private lateinit var queue: RequestQueue
    private  lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PushNotifications.start(getApplicationContext(), "8d9473dd-0a61-4ac4-88de-d5dc18ad095a");
        PushNotifications.addDeviceInterest("shopping")
        PushNotifications.addDeviceInterest("debug-shopping")

        prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") as String
        val id = prefs.getString("id", "") as String

        if (token == "" || id == "") {
            logout()
        } else {
            val tokenProvider = BeamsTokenProvider(
                "https://tranquil-lowlands-73758.herokuapp.com/api/accounts/beams_auth/",
                object: AuthDataGetter {
                    override fun getAuthData(): AuthData {
                        return AuthData(
                            headers = hashMapOf(
                                Pair("Authorization", "Token $token"),
                                Pair("Content-Type", "application/vnd.api+json")
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
                        Log.e("BeamsAuth", "Could not login to Beams: ${error.message}");
                    }

                    override fun onSuccess(vararg values: Void) {
                        Log.i("BeamsAuth", "Beams login success");
                    }
                }
            )
        }

        queue = Volley.newRequestQueue(this)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.fab)

        fab.setOnClickListener {
            intent = Intent(this, AddProduct::class.java)
            this.startActivity(intent)
        }

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
        val itemTouchHelper = ItemTouchHelper(swipeItemCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private var swipeItemCallback: ItemTouchHelper.SimpleCallback = object :
        ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
            Toast.makeText(this@MainActivity, "on Swiped ", Toast.LENGTH_SHORT).show()
            //Remove swiped item from list and notify the RecyclerView
            val index = viewHolder.adapterPosition
            val item = shoppingList[index]
            deleteRequest(item.id)
            shoppingList.removeAt(index)
            adapter.notifyDataSetChanged()
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)!!
            val intrinsicWidth = icon.intrinsicWidth
            val intrinsicHeight = icon.intrinsicHeight
            val background = ColorDrawable()

            val itemView = viewHolder.itemView
            val itemHeight = itemView.bottom - itemView.top

            background.color = Color.RED
            background.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )
            background.draw(c) // Draw on the swiping object

            // Calculate position of delete icon
            val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val iconMargin = (itemHeight - intrinsicHeight) / 2
            val iconLeft = itemView.right - iconMargin - intrinsicWidth
            val iconRight = itemView.right - iconMargin
            val iconBottom = iconTop + intrinsicHeight

            // Draw the delete icon
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            icon.draw(c)
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    private fun parseJson() {
        prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)
        if (token.isNullOrBlank()) {
            intent = Intent(this, Login::class.java)
            this.startActivity(intent)
        }
        val url = "https://tranquil-lowlands-73758.herokuapp.com/api/shoppingitems/"
        val request = object: JsonObjectRequest(
            Method.GET, url, null,
            Response.Listener<JSONObject> { response ->
                val updatedList = ArrayList<ShoppingItem>()
                try {
                    val jsonArray = response.getJSONArray("data")
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        var urlString = jsonObject.getJSONObject("attributes").getString("image")
                        val index = urlString.indexOf(':')
                        val subString = if (index == -1) null else urlString.substring(index + 1)
                        urlString = "https:$subString"

                        updatedList.add(ShoppingItem(
                            id = jsonObject.getString("id"),
                            name = jsonObject.getJSONObject("attributes").getString("name"),
                            imageUrl = urlString,
                            price = jsonObject.getJSONObject("attributes").getString("price")
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

    private fun deleteRequest(id: String) {
        val params = JSONObject()
        val url = "https://tranquil-lowlands-73758.herokuapp.com/api/shoppingitems/$id/"
        prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)

        val deleteRequest = object: JsonObjectRequest(
            Method.DELETE, url, params,
            Response.Listener { _ ->
                Toast.makeText(this, "Item deleted!", Toast.LENGTH_SHORT).show()
            },
            Response.ErrorListener {
                it.printStackTrace()
            }
        )  {
            override fun getHeaders() : Map<String,String> {
                val header = HashMap<String, String>()
                header["Authorization"] = "Token $token"
                header["Content-Type"] = "application/vnd.api+json"
                return header
            }
        }
        queue.add(deleteRequest)
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
            R.id.log_out -> {
                logout()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // This is the main function to take the item selected and create a new fragment with the
    // selected item. This has been given a tag to be able to be identified later although this is
    // not required in this code.
    override fun onItemSelected(position: Int) {
        val shoppingItem = shoppingList[position]
        val fragment = ShoppingItemFragment.newInstance(shoppingItem)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.main_content, fragment, "SHOPPING_ITEM_FRAGMENT")
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun logout() {
        PushNotifications.clearAllState()
        Toast.makeText(this, "Logging out", Toast.LENGTH_SHORT).show()
        intent = Intent(this, Login::class.java)
        this.startActivity(intent)
    }
}
