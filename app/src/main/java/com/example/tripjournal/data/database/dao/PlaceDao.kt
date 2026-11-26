package com.example.tripjournal.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.tripjournal.data.database.entity.Place
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places ORDER BY name COLLATE NOCASE ASC, id ASC")
    fun observeAll(): Flow<List<Place>>

    @Query("SELECT COUNT(*) FROM places")
    fun getPlaceCount(): Flow<Int>

    @Query("SELECT * FROM places WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Place?

    @Insert
    suspend fun insert(place: Place): Long

    @Update
    suspend fun update(place: Place)

    @Delete
    suspend fun delete(place: Place)

    // Cohérence : un lieu déjà utilisé par une visite ne doit pas être supprimé (RESTRICT).
    @Query("SELECT COUNT(*) FROM visits WHERE placeId = :placeId")
    suspend fun countVisits(placeId: Long): Int
}