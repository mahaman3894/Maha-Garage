package maha.garage.domain.engine

import maha.garage.data.local.entity.TireEntity
import org.json.JSONObject

object TireArchiveCodec {
    fun encode(t: TireEntity) = JSONObject().apply {
        put("id", t.id); put("carId", t.carId); put("position", t.position); put("season", t.season)
        put("brand", t.brand); put("productName", t.productName); put("spec", t.spec); put("dot", t.dot)
        put("cumulativeMileage", t.cumulativeMileage); put("installedAtMileage", t.installedAtMileage); put("installDate", t.installDate)
        t.treadDepthMm?.let { put("treadDepthMm", it) }
        put("storageLocation", t.storageLocation ?: ""); put("isMounted", t.isMounted); put("purchaseCost", t.purchaseCost)
        put("notes", t.notes ?: ""); put("setId", t.setId ?: ""); put("assignedWheelPosition", t.assignedWheelPosition ?: ""); put("updatedAt", t.updatedAt)
    }
    fun decode(t: JSONObject) = TireEntity(id = t.getString("id"), carId = t.getString("carId"), position = t.getString("position"),
        season = t.optString("season", "ALL_SEASON"), brand = t.optString("brand"), productName = t.optString("productName"),
        spec = t.optString("spec"), dot = t.optString("dot"), cumulativeMileage = t.optInt("cumulativeMileage"),
        installedAtMileage = t.optInt("installedAtMileage"), installDate = t.optString("installDate"),
        treadDepthMm = if (t.has("treadDepthMm") && !t.isNull("treadDepthMm")) t.getDouble("treadDepthMm") else null,
        storageLocation = t.optString("storageLocation").ifEmpty { null }, isMounted = t.optBoolean("isMounted", true),
        purchaseCost = t.optLong("purchaseCost"), notes = t.optString("notes").ifEmpty { null },
        setId = t.optString("setId").ifEmpty { null }, assignedWheelPosition = t.optString("assignedWheelPosition").ifEmpty { null },
        updatedAt = t.optLong("updatedAt", System.currentTimeMillis()))
}
