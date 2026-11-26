package com.example.tripjournal.ui.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.tripjournal.R
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.ui.trips.TripDates

class TripPagingAdapter(
    private val onClick: (Trip) -> Unit
) : PagingDataAdapter<Trip, TripPagingAdapter.TripViewHolder>(TripDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = getItem(position)
        if (trip != null) {
            holder.bind(trip)
        }
    }

    class TripViewHolder(
        itemView: View,
        private val onClick: (Trip) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.tripItemName)
        private val infoText: TextView = itemView.findViewById(R.id.tripItemInfo)

        fun bind(trip: Trip) {
            nameText.text = trip.name
            infoText.text = itemView.context.getString(
                R.string.trip_info,
                trip.destination,
                TripDates.short(trip.startDate),
                TripDates.short(trip.endDate)
            )
            itemView.setOnClickListener { onClick(trip) }
        }
    }

    object TripDiffCallback : DiffUtil.ItemCallback<Trip>() {
        override fun areItemsTheSame(oldItem: Trip, newItem: Trip): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Trip, newItem: Trip): Boolean = oldItem == newItem
    }
}
