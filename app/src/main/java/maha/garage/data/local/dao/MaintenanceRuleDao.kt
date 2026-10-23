package maha.garage.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import maha.garage.data.local.entity.MaintenanceRuleEntity

@Dao
interface MaintenanceRuleDao {
    @Query("SELECT * FROM maintenance_rules WHERE carId = :carId AND isEnabled = 1")
    fun getRulesForCar(carId: String): Flow<List<MaintenanceRuleEntity>>

    @Query("SELECT * FROM maintenance_rules WHERE carId = :carId")
    suspend fun getAllRulesForCarDirect(carId: String): List<MaintenanceRuleEntity>

    @Query("SELECT * FROM maintenance_rules")
    fun observeAllRules(): Flow<List<MaintenanceRuleEntity>>

    @Query("SELECT * FROM maintenance_rules")
    suspend fun getAllRulesDirect(): List<MaintenanceRuleEntity>

    @Query("SELECT * FROM maintenance_rules WHERE carId = :carId AND itemCode = :itemCode LIMIT 1")
    suspend fun getRuleByItem(carId: String, itemCode: String): MaintenanceRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<MaintenanceRuleEntity>)

    @Update
    suspend fun updateRule(rule: MaintenanceRuleEntity)

    @Query("""
        UPDATE maintenance_rules 
        SET lastServiceDate = :serviceDate,
            lastServiceMileage = :mileage,
            lastServiceHours = :hours,
            lastConditionValue = coalesce(:conditionValue, lastConditionValue)
        WHERE carId = :carId AND itemCode = :itemCode
    """)
    suspend fun updateLastReplacement(
        carId: String,
        itemCode: String,
        serviceDate: String,
        mileage: Int,
        hours: Double? = null,
        conditionValue: String? = null
    )

    @Query("""
        UPDATE maintenance_rules 
        SET lastConditionValue = :conditionValue
        WHERE carId = :carId AND itemCode = :itemCode
    """)
    suspend fun updateLastInspection(
        carId: String,
        itemCode: String,
        conditionValue: String?
    )

    @Query("""
        UPDATE maintenance_rules 
        SET lastServiceDate = :serviceDate,
            lastServiceMileage = :mileage,
            lastServiceHours = :hours,
            lastConditionValue = :conditionValue
        WHERE carId = :carId AND itemCode = :itemCode
    """)
    suspend fun updateLastService(
        carId: String,
        itemCode: String,
        serviceDate: String,
        mileage: Int,
        hours: Double? = null,
        conditionValue: String? = null
    )

    @Query("DELETE FROM maintenance_rules WHERE carId = :carId")
    suspend fun deleteRulesForCar(carId: String)
}
