package maha.garage.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import maha.garage.data.local.entity.MaintenanceCatalogEntity
import maha.garage.data.model.enums.RulePriority

@Dao
interface MaintenanceCatalogDao {
    @Query("SELECT * FROM maintenance_catalog ORDER BY priority ASC, itemCode ASC")
    fun getAllCatalog(): Flow<List<MaintenanceCatalogEntity>>

    @Query("SELECT * FROM maintenance_catalog ORDER BY priority ASC, itemCode ASC")
    suspend fun getAllCatalogDirect(): List<MaintenanceCatalogEntity>

    @Query("SELECT * FROM maintenance_catalog WHERE priority = :priority")
    suspend fun getCatalogByPriority(priority: RulePriority): List<MaintenanceCatalogEntity>

    @Query("SELECT * FROM maintenance_catalog WHERE itemCode = :itemCode LIMIT 1")
    suspend fun getCatalogItem(itemCode: String): MaintenanceCatalogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MaintenanceCatalogEntity>)

    @Query("SELECT COUNT(*) FROM maintenance_catalog")
    suspend fun getCount(): Int
}
