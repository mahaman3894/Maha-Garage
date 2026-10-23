MAHA Garage 날짜 표시/경고색 수정본 1.0.2
기반: 직전 수정본 MahaGarage_v9_Fixed_1.0.1

이번 변경
- 상단의 주행거리·보험·검사 3열을 분리했습니다. 보험/검사 날짜를 생략하지 않는 별도 카드로 표시합니다.
- 보험: 회사명, 가입일, 갱신일(만기일), 남은 일수를 표시합니다.
- 검사: 최근 검사일, 다음 검사 유효기간 만료일, 남은 일수를 표시합니다. 재검사 대상이면 재검사 기한을 우선합니다.
- 한 달보다 많이 남으면 기본색, 날짜 한 달 전부터 당일까지 경고색(앰버), 다음 날부터 빨간색입니다.
  한 달은 고정 30일 대신 달력의 minusMonths(1) 기준입니다. 월말/윤년에도 적용합니다.
- 카드 테두리·배지·기한 날짜에 같은 상태색을 적용합니다. 만기 당일은 '오늘 만료', 다음 날부터 'N일 지남'입니다.
- 내 차고의 보험/검사 요약도 같은 날짜·색상 규칙을 사용합니다.
- 화면이 좁거나 글자 배율이 크면 세로 배치합니다. 넓은 화면은 두 카드를 가로 배치합니다.
  날짜 항목 자체도 공간이 부족하면 세로로 배치합니다. 고정 높이/한 줄 생략으로 날짜를 자르지 않습니다.
- 앱 복귀 및 날짜 변경 시 DB 변경 없이도 색상/남은 날짜를 갱신합니다.
- 저장된 날짜가 없거나 잘못됐으면 '미등록'/중립색입니다. 보험 가입일이나 만기일을 임의로 만들지 않습니다.
- 차량 등록/수정 화면에 실제 보험 만기일 입력란을 추가했습니다. 가입일+1년을 강제로 덮어쓰던 동작을 제거했습니다.
- 검사 유효기간 만료일 기준의 표시색과 기존 최종 수검기한은 구분합니다.
  이번 변경은 카드 표시 규칙입니다. 기존 법정 수검기한 계산/백그라운드 알림 정책은 변경하지 않았습니다.
- 앱 versionCode=3, versionName=1.0.2. Room은 기존 스키마 9 그대로이며 새 데이터 마이그레이션/삭제는 없습니다.
- Firebase 구성 및 이전 1.0.1의 정비/백업/분할 전송 수정은 유지했습니다.

검증
- main/debug 전체 Kotlin·Compose 소스 직접 컴파일 통과.
- 마지막 반응형 컴포넌트 조정 후 해당 파일도 별도로 다시 컴파일해 통과했습니다.
- 단위 테스트 90개 실행, 90개 통과(이전 80개 + 날짜 정책 10개).
  한 달 전 경계, 만료 당일, 지난 날짜, 월말/윤년, 미등록, 회사명만 있는 보험, 재검사 우선 등을 검사했습니다.
- Android Studio 실제 컴포넌트 프리뷰를 360dp/760dp/큰 글씨 배율에 추가했습니다.
- 배치 참고 PNG를 제작하고 글자/날짜 겹침을 확인했습니다. 이 PNG는 실제 앱 화면 캡처가 아닙니다.
- 실제 Android 기기/에뮬레이터 실행 및 APK 빌드/설치는 완료하지 못했습니다.
  직전 실행에서 Gradle의 네이티브/loopback 환경 오류가 있었으며, 이번 검증은 직접 컴파일 방식입니다.
- 실제 DB 스키마 및 Firebase 설정 파일이 직전 수정본과 동일함을 확인했습니다.

사용
ZIP을 새 폴더에 풀고 MahaGarage 폴더를 Android Studio에서 여세요.
기존 앱을 삭제하지 말고 같은 서명으로 업데이트하세요.
날짜가 아직 저장돼 있지 않으면 카드를 눌러 실제 가입일/만기일 또는 검사 결과를 등록하세요.

변경 파일(직전 1.0.1 대비)
app/build.gradle.kts
app/src/debug/java/maha/garage/ui/vehicle/VehicleUiPreviews.kt
app/src/main/java/maha/garage/domain/engine/RenewalDatePolicy.kt
app/src/main/java/maha/garage/domain/engine/VehicleComplianceCalculator.kt
app/src/main/java/maha/garage/ui/components/VehicleInspectionInsuranceSection.kt
app/src/main/java/maha/garage/ui/components/VehicleRenewalCards.kt
app/src/main/java/maha/garage/ui/vehicle/AddEditVehicleScreen.kt
app/src/main/java/maha/garage/ui/vehicle/VehicleDetailScreen.kt
app/src/main/java/maha/garage/ui/vehicle/VehicleListScreen.kt
app/src/test/java/maha/garage/RenewalDatePolicyTest.kt
