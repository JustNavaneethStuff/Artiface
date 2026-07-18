package com.artiface.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaricatureResultDao {

    @Query("SELECT * FROM caricature_results ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<CaricatureResultEntity>>

    @Query("SELECT * FROM caricature_results WHERE isFavourite = 1 ORDER BY createdAtEpochMs DESC")
    fun observeFavourites(): Flow<List<CaricatureResultEntity>>

    @Query("SELECT * FROM caricature_results WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<CaricatureResultEntity?>

    @Query("SELECT * FROM caricature_results WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CaricatureResultEntity?

    @Query("SELECT * FROM caricature_results ORDER BY createdAtEpochMs DESC")
    suspend fun getAll(): List<CaricatureResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CaricatureResultEntity)

    @Query("UPDATE caricature_results SET isFavourite = :favourite WHERE id = :id")
    suspend fun setFavourite(id: String, favourite: Boolean)

    @Query("DELETE FROM caricature_results WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM caricature_results")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM caricature_results")
    suspend fun count(): Int
}
