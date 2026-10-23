package maha.garage.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import maha.garage.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import maha.garage.MahaGarageApp
import maha.garage.ui.dashboard.DashboardScreen
import maha.garage.ui.dashboard.DashboardViewModel
import maha.garage.ui.navigation.Screen
import maha.garage.ui.navigation.bottomNavItems
import maha.garage.ui.service.AddServiceVisitScreen
import maha.garage.ui.service.ServiceHistoryScreen
import maha.garage.ui.service.ServiceViewModel
import maha.garage.ui.settings.SettingsScreen
import maha.garage.ui.theme.*
import maha.garage.ui.vehicle.AddEditVehicleScreen
import maha.garage.ui.vehicle.VehicleDetailScreen
import maha.garage.ui.vehicle.VehicleListScreen
import maha.garage.ui.vehicle.VehicleViewModel

@Composable
fun MainAppScreen(app: MahaGarageApp, targetVehicleId: String? = null, onTargetConsumed: () -> Unit = {}) {
    val navController = rememberNavController()
    LaunchedEffect(targetVehicleId) {
        if (targetVehicleId != null) {
            val car = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                app.database.carDao().getCarByIdDirect(targetVehicleId)
            }
            if (car != null && !car.isArchived) navController.navigate(Screen.VehicleDetail.createRoute(targetVehicleId)) {
                launchSingleTop = true
            }
            onTargetConsumed()
        }
    }

    // 공용 레포지토리 기반 ViewModel 생성
    val dashboardViewModel = remember {
        DashboardViewModel(
            vehicleRepository = app.vehicleRepository,
            fuelRepository = app.fuelRepository,
            preferencesStore = maha.garage.data.preferences.FuelPreferencesStore.getInstance(app)
        )
    }
    val vehicleViewModel = remember {
        VehicleViewModel(app.vehicleRepository, app.maintenanceRepository)
    }
    val serviceViewModel = remember {
        ServiceViewModel(app.maintenanceRepository, app.vehicleRepository)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val topLevelRoutes = listOf(
        Screen.Dashboard.route,
        Screen.Vehicles.route,
        Screen.ServiceHistory.route,
        Screen.Settings.route
    )

    val showBottomBar = currentRoute in topLevelRoutes

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperWhite)
    ) {
        // 앱의 가장 아래쪽 레이어에 배경화면 이미지 배치 (전체 화면 높이/너비 커버)
        Image(
            painter = painterResource(R.drawable.garage_mobile_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = SurfaceWhiteStrong.copy(alpha = 0.94f),
                        tonalElevation = 6.dp
                    ) {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentRoute == item.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SoftInk,
                                    selectedTextColor = SoftInk,
                                    indicatorColor = ToolYellowSoft,
                                    unselectedIconColor = WarmGray,
                                    unselectedTextColor = WarmGray
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Vehicles.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            // 1. 대시보드
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    vehicleViewModel = vehicleViewModel,
                    onNavigateToVehicleDetail = { vehicleId ->
                        navController.navigate(Screen.VehicleDetail.createRoute(vehicleId))
                    },
                    onNavigateToAddVehicle = {
                        navController.navigate(Screen.AddEditVehicle.createRoute(null))
                    },
                    onNavigateToAddService = { vehicleId ->
                        navController.navigate(Screen.AddServiceVisit.createRoute(vehicleId))
                    }
                )
            }

            // 2. 차량 목록
            composable(Screen.Vehicles.route) {
                VehicleListScreen(
                    viewModel = vehicleViewModel,
                    onNavigateToDetail = { vehicleId ->
                        navController.navigate(Screen.VehicleDetail.createRoute(vehicleId))
                    },
                    onNavigateToAddVehicle = {
                        navController.navigate(Screen.AddEditVehicle.createRoute(null))
                    }
                )
            }

            // 3. 차량 상세 정보 (소모품 상태 게이지, 타이어 스왑 패널, 주기 커스텀)
            composable(
                route = Screen.VehicleDetail.route,
                arguments = listOf(
                    navArgument("vehicleId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val vehicleId = backStackEntry.arguments?.getString("vehicleId") ?: ""
                VehicleDetailScreen(
                    vehicleId = vehicleId,
                    viewModel = vehicleViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(Screen.AddEditVehicle.createRoute(id))
                    },
                    onNavigateToAddService = { id ->
                        navController.navigate(Screen.AddServiceVisit.createRoute(id))
                    }
                )
            }

            // 4. 차량 등록 / 수정
            composable(
                route = Screen.AddEditVehicle.route,
                arguments = listOf(
                    navArgument("vehicleId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val vehicleId = backStackEntry.arguments?.getString("vehicleId")
                AddEditVehicleScreen(
                    vehicleId = vehicleId,
                    viewModel = vehicleViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 5. 1회 방문 다중 정비 작업 등록
            composable(
                route = Screen.AddServiceVisit.route,
                arguments = listOf(
                    navArgument("vehicleId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val vehicleId = backStackEntry.arguments?.getString("vehicleId")
                AddServiceVisitScreen(
                    preselectedCarId = vehicleId,
                    viewModel = serviceViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 6. 정비 이력 & 타이어 스왑 로그
            composable(Screen.ServiceHistory.route) {
                ServiceHistoryScreen(
                    viewModel = serviceViewModel,
                    onNavigateToAddService = { vehicleId ->
                        navController.navigate(Screen.AddServiceVisit.createRoute(vehicleId))
                    }
                )
            }

            // 7. 환경설정 & 정보
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToCarHistory = {
                        navController.navigate(Screen.CarHistory.createRoute())
                    }
                )
            }

            // 8. 카 히스토리 (차계부 정비 이력서 & PDF 출력)
            composable(
                route = Screen.CarHistory.route,
                arguments = listOf(
                    navArgument("vehicleId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val vehicleId = backStackEntry.arguments?.getString("vehicleId")
                val carsState by vehicleViewModel.listState.collectAsState()
                maha.garage.ui.history.CarHistoryScreen(
                    cars = carsState.vehiclesWithStatus.map { it.car },
                    loadVisits = { carId, fromDate -> app.maintenanceRepository.getVisitsForPeriod(carId, fromDate) },
                    initialCarId = vehicleId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
}

