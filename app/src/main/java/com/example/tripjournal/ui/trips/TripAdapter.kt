package com.example.tripjournal.ui.trips

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripjournal.R
import com.example.tripjournal.data.database.entity.Trip

class TripAdapter(
    private val onClick: (Trip) -> Unit
) : ListAdapter<Trip, TripAdapter.Holder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.tripItemName)
        private val info: TextView = view.findViewById(R.id.tripItemInfo)

        fun bind(trip: Trip) {
            name.text = trip.name
            info.text = itemView.context.getString(
                R.string.trip_info,
                trip.destination,
                TripDates.short(trip.startDate),
                TripDates.short(trip.endDate)
            )
            itemView.setOnClickListener { onClick(trip) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<Trip>() {
        override fun areItemsTheSame(oldItem: Trip, newItem: Trip) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Trip, newItem: Trip) = oldItem == newItem
    }
}