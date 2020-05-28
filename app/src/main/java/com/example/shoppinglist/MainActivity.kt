package com.example.shoppinglist

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.Image
import android.os.Build
import android.os.Bundle
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
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.net.URL

// The Activity implements AppCompatActivity() class but also the OnItemSelectListener interface
// which links the recyclerView click event to this activity.
class MainActivity : AppCompatActivity(), RecyclerAdapter.OnItemSelectListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecyclerAdapter
    private lateinit var toolbar: Toolbar
    private var shoppingList = ArrayList<ShoppingItem>()
    private lateinit var queue: RequestQueue
    private  lateinit var prefs: SharedPreferences
    private val shoppingItemUrl = "https://tranquil-lowlands-73758.herokuapp.com/api/shoppingitems/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getPreferences(Context.MODE_PRIVATE)

        queue = Volley.newRequestQueue(this)
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
        val request = JsonObjectRequest(
            Request.Method.GET, shoppingItemUrl, null,
            Response.Listener<JSONObject> { response ->
                var updatedList = ArrayList<ShoppingItem>()
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
        )

        val token = prefs.getString("token", null)
        if (token != null) {
            request.headers["Authorization"] = "Token $token"
        }

        queue.add(request)

    }

    private fun deleteRequest(id: String) {
        val params = JSONObject()
        params.put("id", id)
        val deleteUrl = "$shoppingItemUrl$id/"

        val deleteRequest = JsonObjectRequest(
            Request.Method.DELETE, deleteUrl, params,
            Response.Listener<JSONObject> { response ->

            },
            Response.ErrorListener {
                it.printStackTrace()
            }
        )

        val token = prefs.getString("token", null)
        if (token != null) {
            deleteRequest.headers["Authorization"] = "Token $token"
        }
        queue.add(deleteRequest)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun pushRequest(name: String, image: String, price: String) {
        var imageProvided = image
        var priceProvided = price

        if (imageProvided == null) {
            imageProvided = ""
        }
        if (priceProvided == null) {
            priceProvided = ""
        }

        val data = JSONObject(
            """"
            "type" "ShoppingItem",
            "id": "
            """"
        )
        val attributes = JSONObject()
        attributes.put("name", name)
        attributes.put("image", imageProvided)
        attributes.put("price", priceProvided)

        data.put("attributes", attributes)

        val params = JSONObject()
        params.put("data", data)

        val request = JsonObjectRequest(
            Request.Method.POST, shoppingItemUrl, params,
            Response.Listener<JSONObject> { response ->
                // Display the first 500 characters of the response string.
                var updatedList = ArrayList<ShoppingItem>()
                try {
                    val jsonArray = response.getJSONArray("data")
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        var urlString = jsonObject.getJSONObject("attributes").getString("image")
                        val index = urlString.indexOf(':')
                        val subString = if (index == -1) null else urlString.substring(index + 1)
                        urlString = "https:$subString"

//                        val url = URL(urlString)
//                        val bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
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
