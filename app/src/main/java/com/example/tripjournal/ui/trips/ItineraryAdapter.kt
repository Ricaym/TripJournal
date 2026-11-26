package com.example.tripjournal.ui.trips

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tripjournal.R
import com.example.tripjournal.data.database.dao.VisitWithPlace

class ItineraryAdapter(
    private val onVisitClick: (VisitWithPlace) -> Unit
) : ListAdapter<ItineraryRow, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ItineraryRow.DayHeader -> TYPE_HEADER
        is ItineraryRow.VisitItem -> TYPE_VISIT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(inflater.inflate(R.layout.item_day_header, parent, false))
        } else {
            VisitHolder(inflater.inflate(R.layout.item_visit, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is ItineraryRow.DayHeader -> (holder as HeaderHolder).bind(row)
            is ItineraryRow.VisitItem -> (holder as VisitHolder).bind(row.item)
        }
    }

    class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val text: TextView = view.findViewById(R.id.dayHeaderText)
        fun bind(row: ItineraryRow.DayHeader) {
            text.text = itemView.context.getString(
                R.string.itinerary_day, row.day, TripDates.day(row.date)
            )
        }
    }

    inner class VisitHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.visitPlaceName)
        private val location: TextView = view.findViewById(R.id.visitPlaceLocation)
        private val notes: TextView = view.findViewById(R.id.visitNotes)

        fun bind(item: VisitWithPlace) {
            name.text = item.place.name
            location.text = itemView.context.getString(
                R.string.place_location, item.place.city, item.place.country
            )
            notes.text = item.visit.notes
            notes.visibility = if (item.visit.notes.isBlank()) View.GONE else View.VISIBLE
            itemView.setOnClickListener { onVisitClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ItineraryRow>() {
        override fun areItemsTheSame(oldItem: ItineraryRow, newItem: ItineraryRow): Boolean = when {
            oldItem is ItineraryRow.DayHeader && newItem is ItineraryRow.DayHeader ->
                oldItem.day == newItem.day
            oldItem is ItineraryRow.VisitItem && newItem is ItineraryRow.VisitItem ->
                oldItem.item.visit.id == newItem.item.visit.id
            else -> false
        }

        override fun areContentsTheSame(oldItem: ItineraryRow, newItem: ItineraryRow): Boolean =
            oldItem == newItem
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_VISIT = 1
    }
}