package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WashingMachineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MachineDao {
    @Query("SELECT * FROM machines ORDER BY id ASC")
    fun observeAllMachines(): Flow<List<WashingMachineEntity>>

    @Query("SELECT * FROM machines ORDER BY id ASC")
    suspend fun getAllMachinesOnce(): List<WashingMachineEntity>

    @Query("SELECT COUNT(*) FROM machines")
    suspend fun getMachineCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachine(machine: WashingMachineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(machines: List<WashingMachineEntity>)

    @Update
    suspend fun updateMachine(machine: WashingMachineEntity)

    @Update
    suspend fun updateMachines(machines: List<WashingMachineEntity>)

    @Query("DELETE FROM machines WHERE id = :id")
    suspend fun deleteMachineById(id: Int)

    @Query("DELETE FROM machines")
    suspend fun deleteAllMachines()
}
