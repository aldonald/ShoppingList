package com.example.shoppinglist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder


// array list is the items to display and mOnItemSelectListener come from the main activity.
// The mOnItemSelectListener links the click event to the activity.
class RecyclerAdapter(private val shoppingList: ArrayList<ShoppingItem>,
                      private val mOnItemSelectListener: OnItemSelectListener)
    : RecyclerView.Adapter<RecyclerAdapter.ShoppingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapter.ShoppingViewHolder {
        // The following inflates the view.
        val itemView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.recycler_item_row, parent, false)

        return ShoppingViewHolder(itemView, mOnItemSelectListener)
    }

    override fun getItemCount(): Int = shoppingList.size

    // This function binds the current item from the passed in list to the recycler view when it
    // needs to be displayed (it keeps a limited number of rows off the screen also before recycling
    // the holder for a new item in the list).
    override fun onBindViewHolder(holder: RecyclerAdapter.ShoppingViewHolder, position: Int) {
        val currentItem = shoppingList[position]

        holder.textView.text = currentItem.name

    }

    fun updateShoppingList(newList: ArrayList<ShoppingItem>) {
        shoppingList.clear()
        shoppingList.addAll(newList)
        this.notifyDataSetChanged()
    }

    // The main activity OnItemSelectListener passed in when creating the view holder for each item
    class ShoppingViewHolder(v: View, private val selectListener: OnItemSelectListener)
        : RecyclerView.ViewHolder(v), View.OnClickListener {
        // This is where the image and text is displayed for each row
        val textView: TextView = v.findViewById(R.id.item_name)

        init {
            // sets the onclick listener on initialisation of each row
            v.setOnClickListener(this)
        }

        // This function takes the received click and passes the click back to the function on the
        // main activity
        override fun onClick(v: View?) {
            if (v != null) {
                selectListener.onItemSelected(this.adapterPosition)
            }
        }
    }

    // The interface allows for parent activity to set the activity when the click is received by
    // the holder of each row
    interface OnItemSelectListener {
        fun onItemSelected(position: Int)
    }


}
