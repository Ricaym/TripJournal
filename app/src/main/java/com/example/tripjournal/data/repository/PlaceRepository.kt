package com.example.tripjournal.data.repository

import com.example.tripjournal.data.database.dao.PlaceDao
import com.example.tripjournal.data.database.entity.Place
import com.example.tripjournal.domain.VisitRules
import kotlinx.coroutines.flow.Flow

/** Résultat d'une suppression de lieu : un lieu utilisé par des visites est protégé. */
sealed class PlaceDeleteResult {
    object Deleted : PlaceDeleteResult()
    data class InUse(val visitCount: Int) : PlaceDeleteResult()
}

class PlaceRepository(private val dao: PlaceDao) {

    fun observePlaces(): Flow<List<Place>> = dao.observeAll()

    suspend fun getPlace(id: Long): Place? = dao.getById(id)

    /** Ajoute (id = 0) ou modifie un lieu. Renvoie son id. */
    suspend fun save(place: Place): Long {
        require(VisitRules.isValidPlace(place)) { "Nom, ville et pays sont obligatoires" }
        return if (place.id == 0L) dao.insert(place) else { dao.update(place); place.id }
    }

    /** Cohérence : on refuse de supprimer un lieu encore utilisé par des visites. */
    suspend fun delete(place: Place): PlaceDeleteResult {
        val used = dao.countVisits(place.id)
        if (used > 0) return PlaceDeleteResult.InUse(used)
        dao.delete(place)
        return PlaceDeleteResult.Deleted
    }
}
