package com.example.tripjournal.ui.places

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripjournal.R
import com.example.tripjournal.data.database.entity.Place

/** Affiche la liste des lieux. Un clic sur une ligne ouvre le lieu en modification. */
class PlaceAdapter(private val onClick: (Place) -> Unit) :
    ListAdapter<Place, PlaceAdapter.Holder>(DIFF) {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.placeName)
        private val location: TextView = view.findViewById(R.id.placeLocation)
        private val description: TextView = view.findViewById(R.id.placeDescription)

        fun bind(place: Place, onClick: (Place) -> Unit) {
            name.text = place.name
            location.text = itemView.context.getString(R.string.place_location, place.city, place.country)
            description.text = place.description
            description.visibility = if (place.description.isBlank()) View.GONE else View.VISIBLE
            itemView.setOnClickListener { onClick(place) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_place, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) =
        holder.bind(getItem(position), onClick)

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Place>() {
            override fun areItemsTheSame(oldItem: Place, newItem: Place) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Place, newItem: Place) = oldItem == newItem
        }
    }
}