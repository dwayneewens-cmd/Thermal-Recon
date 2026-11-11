package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ThermalReportDao {
    @Query("SELECT * FROM thermal_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ThermalReportEntity>>

    @Query("SELECT * FROM thermal_reports WHERE id = :reportId")
    suspend fun getReportById(reportId: Long): ThermalReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ThermalReportEntity): Long

    @Update
    suspend fun updateReport(report: ThermalReportEntity)

    @Delete
    suspend fun deleteReport(report: ThermalReportEntity)

    @Query("DELETE FROM thermal_reports")
    suspend fun clearAllReports()
}
