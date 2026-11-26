package com.example.tripjournal.data.repository

import com.example.tripjournal.data.database.dao.VisitDao
import com.example.tripjournal.data.database.dao.VisitWithPlace
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.database.entity.Visit
import com.example.tripjournal.domain.VisitRules
import kotlinx.coroutines.flow.Flow

class VisitRepository(private val dao: VisitDao) {

    /** Itinéraire d'un voyage : se met à jour tout seul après chaque modification. */
    fun observeItinerary(tripId: Long): Flow<List<VisitWithPlace>> = dao.observeForTrip(tripId)

    suspend fun getVisit(id: Long): Visit? = dao.getById(id)

    /** Ajoute (id = 0) ou modifie une visite, uniquement si elle est cohérente avec son voyage. */
    suspend fun save(visit: Visit, trip: Trip): Long {
        require(visit.tripId == trip.id) { "La visite n'appartient pas à ce voyage" }
        require(VisitRules.isWithinTrip(visit.visitDate, trip)) { "La date doit être comprise dans le voyage" }
        return if (visit.id == 0L) dao.insert(visit) else { dao.update(visit); visit.id }
    }

    suspend fun delete(visit: Visit) = dao.delete(visit)
}