package maha.garage.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Vehicles : Screen("vehicles")
    object VehicleDetail : Screen("vehicle_detail/{vehicleId}") {
        fun createRoute(vehicleId: String) = "vehicle_detail/$vehicleId"
    }
    object AddEditVehicle : Screen("add_edit_vehicle?vehicleId={vehicleId}") {
        fun createRoute(vehicleId: String? = null) = if (vehicleId != null) "add_edit_vehicle?vehicleId=$vehicleId" else "add_edit_vehicle"
    }
    object AddServiceVisit : Screen("add_service_visit?vehicleId={vehicleId}") {
        fun createRoute(vehicleId: String? = null) = if (vehicleId != null) "add_service_visit?vehicleId=$vehicleId" else "add_service_visit"
    }
    object ServiceHistory : Screen("service_history")
    object Settings : Screen("settings")
    object CarHistory : Screen("car_history?vehicleId={vehicleId}") {
        fun createRoute(vehicleId: String? = null) = if (vehicleId != null) "car_history?vehicleId=$vehicleId" else "car_history"
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        title = "연비/주유",
        route = Screen.Dashboard.route,
        selectedIcon = Icons.Filled.LocalGasStation,
        unselectedIcon = Icons.Outlined.LocalGasStation
    ),
    BottomNavItem(
        title = "내 차량",
        route = Screen.Vehicles.route,
        selectedIcon = Icons.Filled.DirectionsCar,
        unselectedIcon = Icons.Outlined.DirectionsCar
    ),
    BottomNavItem(
        title = "정비 이력",
        route = Screen.ServiceHistory.route,
        selectedIcon = Icons.Filled.Build,
        unselectedIcon = Icons.Outlined.Build
    ),
    BottomNavItem(
        title = "설정",
        route = Screen.Settings.route,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)
