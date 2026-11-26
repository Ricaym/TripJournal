package com.example.tripjournal.data.repository

import com.example.tripjournal.data.database.dao.PlaceDao
import com.example.tripjournal.data.database.dao.TripDao
import com.example.tripjournal.data.database.dao.VisitDao
import com.example.tripjournal.domain.TravelStats
import com.example.tripjournal.domain.TravelStatsCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Combine les trois tables : les statistiques sont recalculées automatiquement
 * dès qu'un voyage, un lieu ou une visite est ajouté, modifié ou supprimé.
 */
class StatisticsRepository(
    private val tripDao: TripDao,
    private val visitDao: VisitDao,
    private val placeDao: PlaceDao,
    private val clock: () -> Long = System::currentTimeMillis
) {
    fun observeStats(): Flow<TravelStats> = combine(
        tripDao.getAllTrips(),
        visitDao.observeAllWithPlace(),
        placeDao.getPlaceCount()
    ) { trips, visits, placeCount ->
        TravelStatsCalculator.compute(trips, visits, placeCount, clock())
    }
}
