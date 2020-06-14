package com.aldonaldshopping.shoppinglist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
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
    private lateinit var prefs: SharedPreferences
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // When accessing the main activity, the app resets with Beams and firebase to ensure push notifications are connected.
        PushNotifications.start(getApplicationContext(), "8d9473dd-0a61-4ac4-88de-d5dc18ad095a")
        // The following is a channel which would allow broad based user messages.
        PushNotifications.addDeviceInterest("shopping")
        PushNotifications.addDeviceInterest("debug-shopping")

        // Access local storage for the use of the token which demonstrates the user is logged in -
        // this token will need to be sent with all requests to the server.
        prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", "") as String
        val id = prefs.getString("id", "") as String

        if (token == "" || id == "") {
            // Ensure logged in and id present so linked to Beams
            logout()
        } else {
            // Register with Beams - Beams recommends this is done on every log in.
            // The first part is a request built for Beams to be able to verify the user when the
            // request goes through to them below. It is Beams who will use this get request.
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

            // This is the function to set the user up in Beams.
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

        // This is the queue for all internet requests.
        queue = Volley.newRequestQueue(this)

        // Below sets up the banner at the top of the app - this is custom so that it allows me to
        // put a menu in the bar.
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerView)

        // The fab is my plus sign floating action bar. This is set to show as when the user comes
        // from the add item screen, it is hidden.
        fab = findViewById(R.id.fab)
        fab.show()

        fab.setOnClickListener {
            intent = Intent(this, AddProduct::class.java)
            fab.hide()
            this.startActivity(intent)
        }

        // The following is to allow the user to swipe down and refresh the items from the server.
        // Might be useful if they have multiple devices or just peace of mind that everything is up
        // to date.
        swipeRefreshLayout = findViewById(R.id.swiperefresh)
        swipeRefreshLayout.setOnRefreshListener {
            // This is the main fetch call for shopping items below.
            parseJson()
        }

        // Fetch all the items.
        parseJson()

        // Below finds the recyclerView from the layout and attaches the adapter and passes the
        // OnItemSelectListener which is used by this Activity as an interface in order to link the
        // click event to the parent activity for the fragment.
        recyclerView = findViewById(R.id.recyclerView)
        adapter = RecyclerAdapter(shoppingList, this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(false)
        val itemTouchHelper = ItemTouchHelper(swipeItemCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    // This function sets up the recycler to have swipable delete rows. In the passed in variables
    // I have specified I only want to swipe left - this is due to the difficulty in paining the
    // red and having the bin drawable.
    private var swipeItemCallback: ItemTouchHelper.SimpleCallback = object :
        ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
        // I don't want to use this functionality so return false.
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        // When the item is swiped it calls the deleteRequest function from below.
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
            Toast.makeText(this@MainActivity, "Item deleted! ", Toast.LENGTH_SHORT).show()
            //Remove swiped item from list and notify the RecyclerView
            val index = viewHolder.adapterPosition
            val item = shoppingList[index]
            deleteRequest(item.id)

            // Now we update the local list and update the recycler view.
            shoppingList.removeAt(index)
            adapter.notifyDataSetChanged()
        }

        // This function is to draw the red onto the row as it is swiped
        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            // This is the bin icin
            val icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)!!

            // Find out the height and width of the icon
            val intrinsicWidth = icon.intrinsicWidth
            val intrinsicHeight = icon.intrinsicHeight

            // Create the background
            val background = ColorDrawable()

            // Get the height of the row
            val itemView = viewHolder.itemView
            val itemHeight = itemView.bottom - itemView.top

            // Set the red background
            background.color = Color.RED

            // Set the amount to colour - from the right hand edge to the right hand edge plus the
            // amount the element has moved dX. Also from the top to the bottom.
            background.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )

            // Draw on the row
            background.draw(c)

            // Calculate where the bin will be placed in the row using the row measurements and the
            // icon measurements from earlier.
            val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
            val iconMargin = (itemHeight - intrinsicHeight) / 2
            val iconLeft = itemView.right - iconMargin - intrinsicWidth
            val iconRight = itemView.right - iconMargin
            val iconBottom = iconTop + intrinsicHeight

            // Draw the bin delete icon
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            icon.draw(c)
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    // This is the function to retrieve the shopping items from the server.
    private fun parseJson() {
        // This sets the small spinning arrow while the data is being fetched.
        swipeRefreshLayout.isRefreshing = true

        // Get the auth token for the request.
        prefs = getSharedPreferences("auth", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)
        if (token.isNullOrBlank()) {
            // No token so need to log in.
            intent = Intent(this, Login::class.java)
            this.startActivity(intent)
        }

        // The request is built as an object so that headers including the auth token and the
        // content type can be added to the request. More is explained on the content type in create
        // user requests.
        val url = "https://tranquil-lowlands-73758.herokuapp.com/api/shoppingitems/"
        val request = object: JsonObjectRequest(
            Method.GET, url, null,
            Response.Listener<JSONObject> { response ->
                // When the request is successful, we create a new list and populate the list with
                // the items from the JSON response - creating a list of new ShoppingItems.
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
                    // The adapter is updated with the new list and notified of change.
                    // The spinning arrow is stopped.
                    adapter.updateShoppingList(updatedList)
                    adapter.notifyDataSetChanged()
                    swipeRefreshLayout.isRefreshing = false
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
        // This is the request added to the Volley queue. The queue is async which is why a lot of
        // the logic goes into the response when it depends on the outcome of the request.
        queue.add(request)
    }

    // This is the function called to delete an item when the row has been swiped. It is set up the
    // same way as other requests with a different endpoint for a detail view of the item and also
    // a delete method passed into the Json request.
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

    // This function adds my menu (only 1 item at the moment) to the menu bar.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu to use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    // When logout is selected from the options bar menu we call the logout function.
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
        fab.hide()
        val shoppingItem = shoppingList[position]
        val fragment = ShoppingItemFragment.newInstance(shoppingItem)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.main_content, fragment, "SHOPPING_ITEM_FRAGMENT")
        transaction.addToBackStack(null)
        transaction.commit()
    }

    // Function called to log the user out, clear the token from local storage and remove
    // push notifications. This will prevent auto log in if the user leaves the app after clicking
    // logout.
    private fun logout() {
        PushNotifications.clearAllState()
        val token = prefs.getString("token", "") as String
        if (token != "") {
            with (prefs.edit()) {
                putString("token", "")
                commit()
            }
        }

        Toast.makeText(this, "Logging out", Toast.LENGTH_SHORT).show()
        intent = Intent(this, Login::class.java)
        this.startActivity(intent)
    }

    // This ensures the floating action bar returns when the fragment is closed.
    override fun onBackPressed() {
        super.onBackPressed()
        fab.show()
    }
}
