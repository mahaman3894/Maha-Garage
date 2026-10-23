package maha.garage.ui.theme

import androidx.compose.ui.graphics.Color

// MahaGarage UI v3 — 밝은 종이 배경 + 고불투명 화이트 카드 + 생생한 툴 포인트 팔레트 (ui.md 준수)
val PaperWhite = Color(0xFFF7F7F5)          // 앱 전체 바탕 페이퍼 화이트
val SurfaceWhite = Color(0xFAFFFFFF)        // 고불투명 화이트 (98% alpha) - 배경 일러스트 비침 방지
val SurfaceWhiteStrong = Color(0xFDFFFFFF)  // 고불투명 화이트 강조 (99% alpha) - 제목/핵심 카드 선명도 극대화
val SurfaceWhiteSoft = Color(0xF2FFFFFF)    // 반투명 화이트 연한 면 (95% alpha)

// 경쾌한 스케치 & 공구 포인트 컬러 (ui.md)
val SketchRed = Color(0xFFE76B61)           // 주요 행동, 교체 필요, 소량 강조
val SketchRedSoft = Color(0xFFFFF0EC)       // 연한 코랄 보조 면
val ToolYellow = Color(0xFFF2C75C)          // 보조 버튼, 알림, 카탈로그 외곽 강조
val ToolYellowSoft = Color(0xFFFFF8DD)      // 연한 크림 선택/안내 면
val ToolOrange = Color(0xFFEDA15F)          // 보조 동작, 정비·기록 아이콘
val ToolOrangeSoft = Color(0xFFFFF2E6)      // 연한 살구 보조 면
val JackGreen = Color(0xFF7DBB76)           // 완료, 양호, 등록·확인 보조
val JackGreenSoft = Color(0xFFEFF8EC)       // 양호 배지 및 연한 그린 면

// 텍스트 & 경계선 (소프트 잉크 & 웜 그레이)
val SoftInk = Color(0xFF353530)             // 본문·숫자·제목 기본 글자
val WarmGray = Color(0xFF65655E)            // 설명·날짜·단위 보조 글자
val WarmLine = Color(0x38585548)            // rgba(88,85,72,.22) 카드 경계·구분선

// Primary Brand Colors (밝고 경쾌한 워크숍 테마)
val GaragePrimary = ToolYellow
val GaragePrimaryLight = Color(0xFFF5CF72)
val GaragePrimaryDark = Color(0xFFDCA938)
val GarageSecondary = SoftInk
val GarageAccent = ToolOrange

// Status Colors (ui.md 상태 매핑: 초록=양호, 노랑/오렌지=교체예정/점검, 빨강=교체필요)
val StatusOverdue = SketchRed               // 교체 필요 (스케치 레드)
val StatusDueSoon = ToolYellow              // 교체 예정 (툴 옐로)
val StatusInspection = ToolOrange           // 상태 점검 (툴 오렌지)
val StatusNormal = JackGreen                // 정상/양호 (잭 그린)
val StatusNeutral = Color(0xFF8C8C84)       // 대기/미확인

// 하위 호환 매핑: 기존 SurfaceDark/BackgroundDark 등을 참조하던 화면들이 자동으로 밝은 종이 테마를 따르도록 매핑
val BackgroundDark = PaperWhite
val SurfaceDark = SurfaceWhite
val SurfaceVariantDark = SurfaceWhiteStrong
val BorderDark = WarmLine

val BackgroundLight = PaperWhite
val SurfaceLight = SurfaceWhite
val SurfaceVariantLight = SurfaceWhiteStrong
val BorderLight = WarmLine

val TextPrimaryDark = SoftInk
val TextSecondaryDark = WarmGray
val TextPrimaryLight = SoftInk
val TextSecondaryLight = WarmGray

