package maha.garage.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch
import maha.garage.MahaGarageApp
import maha.garage.ui.tutorial.TutorialExperience
import maha.garage.ui.tutorial.TutorialPreferences

@Composable
fun MainAppScreen(app: MahaGarageApp, targetVehicleId: String? = null, onTargetConsumed: () -> Unit = {}) {
    val preferences = remember(app) { TutorialPreferences(app) }
    var active by rememberSaveable { mutableStateOf(false) }
    var invitation by rememberSaveable { mutableStateOf(!preferences.completed) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun finish() {
        if (saving) return
        saving = true
        scope.launch {
            try { preferences.finish(); active = false; invitation = false; error = null }
            catch (e: Exception) { error = e.message; active = false; invitation = true }
            finally { saving = false }
        }
    }
    if (active) TutorialExperience(app, onExit = { finish() })
    else GarageContent(app, targetVehicleId, onTargetConsumed, onStartTutorial = { active = true })
    if (invitation && !active) {
        AlertDialog(onDismissRequest = { finish() }, title = { Text("튜토리얼을 보시겠습니까?") },
            text = { Text(error ?: "실제 화면에서 차량 등록부터 주유·긴급출동·정비 이력까지 연습해 보세요. 예시 데이터는 개인 기록에 저장되지 않습니다.") },
            confirmButton = { Button(onClick = { invitation = false; active = true }, enabled = !saving) { Text("튜토리얼 시작") } },
            dismissButton = { TextButton(onClick = { finish() }, enabled = !saving) { Text("건너뛰기") } })
    }
}
