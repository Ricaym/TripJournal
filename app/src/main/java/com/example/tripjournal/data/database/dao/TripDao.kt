package com.example.tripjournal.data.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tripjournal.data.database.entity.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startDate DESC, id DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE name LIKE '%' || :query || '%' OR destination LIKE '%' || :query || '%' ORDER BY startDate DESC, id DESC")
    fun getTripsPaged(query: String): PagingSource<Int, Trip>

    @Query("SELECT * FROM trips WHERE name LIKE '%' || :query || '%' OR destination LIKE '%' || :query || '%' ORDER BY startDate ASC, id ASC")
    fun getTripsPagedByDateAsc(query: String): PagingSource<Int, Trip>

    @Query("SELECT * FROM trips WHERE name LIKE '%' || :query || '%' OR destination LIKE '%' || :query || '%' ORDER BY name COLLATE LOCALIZED ASC, id DESC")
    fun getTripsPagedByName(query: String): PagingSource<Int, Trip>

    @Query("SELECT * FROM trips WHERE name LIKE '%' || :query || '%' OR destination LIKE '%' || :query || '%' ORDER BY destination COLLATE LOCALIZED ASC, id DESC")
    fun getTripsPagedByDestination(query: String): PagingSource<Int, Trip>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripById(tripId: Long): Flow<Trip?>

    @Query("SELECT COUNT(*) FROM trips")
    fun getTripCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrip(trip: Trip): Long

    // Insertion groupée (une seule transaction) : utilisée par l'import JSON initial.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrips(trips: List<Trip>): List<Long>

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)
}
