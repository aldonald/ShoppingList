package com.example.shoppinglist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class ShoppingItemFragment : Fragment() {
    private lateinit var selectedItem: ShoppingItem

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // I made the news source serializable so I could pass to the fragment.
        selectedItem = arguments!!.getSerializable("item") as ShoppingItem

        // Inflating the view.
        val view = inflater!!.inflate(R.layout.shopping_item_fragment, container, false)
        // Setting the picture from the passed in news source
        val imageView = view.findViewById<ImageView>(R.id.shopping_item_image)
        val itemName = view.findViewById<TextView>(R.id.shopping_item_name)
        val itemPrice = view.findViewById<TextView>(R.id.shopping_item_price)
        // imageView.setImageResource(selectedItem.imageResource)
        itemName.text = selectedItem.name
        itemPrice.text = "$${selectedItem.price}"
        Picasso.get().load(selectedItem.imageUrl)
            .resize(150, 150)
            .centerCrop()
            .into(imageView)

        return view
    }

    companion object {
        // This is the function we can call from the parent allowing us to pass in arguments to the
        // fragment creation process.
        fun newInstance(item: ShoppingItem): ShoppingItemFragment {
            val fragment = ShoppingItemFragment()

            // Supply index input as an argument.
            val args = Bundle()
            // We serialize the item to pass it through.
            args.putSerializable("item", item)
            fragment.arguments = args

            return fragment
        }
    }
}