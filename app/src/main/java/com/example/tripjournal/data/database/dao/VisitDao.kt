package com.example.tripjournal.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.example.tripjournal.data.database.entity.Place
import com.example.tripjournal.data.database.entity.Visit
import kotlinx.coroutines.flow.Flow

/** Une visite avec son lieu : c'est ce que l'écran d'itinéraire affiche. */
data class VisitWithPlace(
    @Embedded val visit: Visit,
    @Relation(parentColumn = "placeId", entityColumn = "id")
    val place: Place
)

@Dao
interface VisitDao {
    // Itinéraire d'un voyage : visites triées par date, avec leur lieu.
    @Transaction
    @Query("SELECT * FROM visits WHERE tripId = :tripId ORDER BY visitDate ASC, id ASC")
    fun observeForTrip(tripId: Long): Flow<List<VisitWithPlace>>

    @Query("SELECT * FROM visits WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Visit?

    @Insert
    suspend fun insert(visit: Visit): Long

    @Update
    suspend fun update(visit: Visit)

    @Delete
    suspend fun delete(visit: Visit)
}
