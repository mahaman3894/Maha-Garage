package maha.garage.data.local.converter

import androidx.room.TypeConverter
import maha.garage.data.model.enums.*

class Converters {
    @TypeConverter
    fun fromPowertrain(value: PowertrainType?): String? = value?.name

    @TypeConverter
    fun toPowertrain(value: String?): PowertrainType? =
        value?.let { runCatching { PowertrainType.valueOf(it) }.getOrDefault(PowertrainType.GASOLINE) }

    @TypeConverter
    fun fromVehicleClass(value: VehicleClass?): String? = value?.name

    @TypeConverter
    fun toVehicleClass(value: String?): VehicleClass? =
        value?.let { runCatching { VehicleClass.valueOf(it) }.getOrDefault(VehicleClass.SEDAN) }

    @TypeConverter
    fun fromActionType(value: ActionType?): String? = value?.name

    @TypeConverter
    fun toActionType(value: String?): ActionType? =
        value?.let { runCatching { ActionType.valueOf(it) }.getOrDefault(ActionType.REPLACE) }

    @TypeConverter
    fun fromRulePriority(value: RulePriority?): String? = value?.name

    @TypeConverter
    fun toRulePriority(value: String?): RulePriority? =
        value?.let { runCatching { RulePriority.valueOf(it) }.getOrDefault(RulePriority.P1) }

    @TypeConverter
    fun fromSourceType(value: SourceType?): String? = value?.name

    @TypeConverter
    fun toSourceType(value: String?): SourceType? =
        value?.let { runCatching { SourceType.valueOf(it) }.getOrDefault(SourceType.GENERIC_ESTIMATE) }

    @TypeConverter
    fun fromPowertrainList(list: List<PowertrainType>?): String =
        list?.joinToString(",") { it.name } ?: ""

    @TypeConverter
    fun toPowertrainList(value: String?): List<PowertrainType> =
        if (value.isNullOrBlank()) emptyList()
        else value.split(",").mapNotNull { name ->
            runCatching { PowertrainType.valueOf(name.trim()) }.getOrNull()
        }

    @TypeConverter
    fun fromVehicleClassList(list: List<VehicleClass>?): String =
        list?.joinToString(",") { it.name } ?: ""

    @TypeConverter
    fun toVehicleClassList(value: String?): List<VehicleClass> =
        if (value.isNullOrBlank()) emptyList()
        else value.split(",").mapNotNull { name ->
            runCatching { VehicleClass.valueOf(name.trim()) }.getOrNull()
        }

    @TypeConverter
    fun fromStringList(list: List<String>?): String =
        list?.joinToString(";;") ?: ""

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList()
        else value.split(";;").map { it.trim() }
}
