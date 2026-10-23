package maha.garage.data.catalog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class VehicleCatalogItem(
    val id: String,
    val brand: String,
    val modelName: String,
    val lineup: String,
    val trim: String,
    val modelYear: Int,
    val powertrain: String,
    val vehicleClass: String,
    val timingType: String,
    val timingDesc: String,
    val displacement: String,
    val power: String,
    val torque: String,
    val transmission: String,
    val driveSystem: String,
    val fuelEconomy: String,
    val dimensions: String
) {
    val searchEntry by lazy { VehicleSearchIndex.entry(brand, modelName, lineup, trim) }
}

object VehicleCatalogHelper {

    @Volatile
    private var cachedCatalog: List<VehicleCatalogItem>? = null

    suspend fun getCatalog(context: Context): List<VehicleCatalogItem> {
        cachedCatalog?.let { return it }
        return withContext(Dispatchers.IO) {
            cachedCatalog?.let { return@withContext it }
            val list = mutableListOf<VehicleCatalogItem>()
            try {
                context.assets.open("vehicle_catalog.json").bufferedReader().use { reader ->
                    val jsonStr = reader.readText()
                    val jsonArray = JSONArray(jsonStr)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            VehicleCatalogItem(
                                id = obj.optString("id"),
                                brand = obj.optString("brand"),
                                modelName = obj.optString("modelName"),
                                lineup = obj.optString("lineup"),
                                trim = obj.optString("trim"),
                                modelYear = obj.optInt("modelYear", 2022),
                                powertrain = obj.optString("powertrain", "GASOLINE"),
                                vehicleClass = obj.optString("vehicleClass", "SEDAN"),
                                timingType = obj.optString("timingType", "CHAIN"),
                                timingDesc = obj.optString("timingDesc"),
                                displacement = obj.optString("displacement"),
                                power = obj.optString("power"),
                                torque = obj.optString("torque"),
                                transmission = obj.optString("transmission"),
                                driveSystem = obj.optString("driveSystem"),
                                fuelEconomy = obj.optString("fuelEconomy"),
                                dimensions = obj.optString("dimensions")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            cachedCatalog = list
            list
        }
    }

    /**
     * 브랜드 및 모델명 키워드로 카탈로그 검색 (알파벳 검색조건 시 벤츠 차량 최우선 정렬)
     */
    fun search(catalog: List<VehicleCatalogItem>, query: String, selectedBrand: String? = null): List<VehicleCatalogItem> {
        val tokens = VehicleSearchIndex.tokens(query)
        val filtered = catalog.filter { item ->
            val matchBrand = if (selectedBrand.isNullOrBlank() || selectedBrand == "전체") {
                true
            } else {
                item.brand.equals(selectedBrand, ignoreCase = true)
            }
            if (!matchBrand) return@filter false

            VehicleSearchIndex.matches(item.searchEntry, tokens)
        }

        if (tokens.isEmpty()) return filtered

        val hasAlphabetToken = tokens.any { token -> token.any { c -> c in 'a'..'z' } }
        val containsBenzInResult = filtered.any { VehicleSearchIndex.isBenz(it.brand) }

        // 알파벳 검색조건이거나 결과에 벤츠 차량이 포함된 경우 벤츠 최우선 정렬
        if (!hasAlphabetToken && !containsBenzInResult) {
            return filtered
        }

        return filtered.sortedWith { a, b ->
            val aBenz = VehicleSearchIndex.isBenz(a.brand)
            val bBenz = VehicleSearchIndex.isBenz(b.brand)

            // 1. 벤츠 차량 최우선 (대소문자 무관)
            if (aBenz != bBenz) {
                return@sortedWith if (aBenz) -1 else 1
            }

            // 2. 검색 토큰과의 일치도 (벤츠 내에서 해당 알파벳 클래스 직계 모델 우선)
            val aScore = calculateMatchScore(a, tokens)
            val bScore = calculateMatchScore(b, tokens)
            if (aScore != bScore) {
                return@sortedWith aScore.compareTo(bScore)
            }

            // 3. 최신 연식 내림차순
            val yearDiff = b.modelYear.compareTo(a.modelYear)
            if (yearDiff != 0) return@sortedWith yearDiff

            // 4. 모델명 순
            a.modelName.compareTo(b.modelName)
        }
    }

    private fun calculateMatchScore(item: VehicleCatalogItem, tokens: List<String>): Int {
        val isBenz = VehicleSearchIndex.isBenz(item.brand)
        if (isBenz) {
            val exactAlias = tokens.any { tok -> tok in item.searchEntry.aliases }
            if (exactAlias) return 0

            val modelMatch = tokens.any { tok ->
                item.searchEntry.model.startsWith(tok) || item.searchEntry.model.contains(tok)
            }
            if (modelMatch) return 1

            val fieldMatch = tokens.any { tok ->
                item.searchEntry.fields.any { it.contains(tok) }
            }
            if (fieldMatch) return 2

            return 3
        } else {
            val modelStarts = tokens.any { tok -> item.searchEntry.model.startsWith(tok) }
            if (modelStarts) return 0
            return 1
        }
    }
}
