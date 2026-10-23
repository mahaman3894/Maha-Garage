# Multi-Vehicle Maintenance Manager — Android 앱 개발 명세서

- 문서 버전: 1.0
- 작성일: 2026-10-16
- 대상: Android Studio / Kotlin / Jetpack Compose
- 앱 목적: 여러 대의 차량 소모품 및 정비 이력을 기록하고, **차량 사양과 운행조건에 맞는 교체·점검 알림**을 제공한다.
- 목표 시장: 대한민국, 주행거리 km, 날짜 yyyy-MM-dd, 금액 KRW

> **중요 — 안전 및 데이터 품질:** 아래 기본 주기는 **앱 초기 추천값/관리 후보**이며 특정 차량에 대한 제조사 확정 정비지침이 아니다. 차종·연식·엔진·시장·옵션·운행조건에 따라 크게 다르다. 안전 관련 부품은 수치만으로 교체 판정하지 않는다. 제조사 정비표를 입력하거나 사용자가 확인할 때에만 `VERIFIED_OEM`으로 표시한다. 미검증 항목은 `GENERIC_ESTIMATE`, 점검·상태 기준 항목은 `CONDITION_BASED`로 표시한다. 값이 없는 항목은 억지로 주기를 설정하지 않는다.

---

## 1. 제품 비전 및 범위

핵심 가치: **내가 관리하는 모든 차량에서 무엇을, 언제, 왜 점검 또는 교체해야 하는지 즉시 알 수 있게 한다.**

### MVP 필수 기능
1. 차량 여러 대 등록 / 편집 / 비활성화, 차량별 대표 사진과 별명.
2. 연료·동력 유형과 차체·용도 유형을 독립적으로 선택.
3. 소모품 템플릿 자동 추천, 적용·제외 여부 확인, 제조사 규격과 교환주기 사용자 수정.
4. 마지막 점검/교체 날짜와 주행거리 저장, 각 품목의 다음 예정 시점 계산.
5. 로컬 알림 및 통합 '오늘의 관리' 화면: 예정, 초과, 확인 필요, 정상.
6. 정비 방문 1건에 여러 작업과 비용/사진/영수증 연결.
7. 주행거리 갱신 및 월평균 주행거리 기반 **예상** 교체일 제공.
8. 로컬 오프라인 저장, 내보내기/가져오기(JSON/CSV).

### 확장 기능
- 차량별 정비비 통계, 보험·검사·보증 만료 알림, 주유/충전 내역, 연비·전비.
- 클라우드 동기화, 가족 공동관리, 제조사 정비표 패키지 업데이트, OBD-II 연동.
- 엔진 가동시간 및 트럭 운송·고하중 운행 기준 관리.

## 2. 차량 등록 스키마 — 2축 이상 분리

**연료/파워트레인(`powertrainType`)**
- `GASOLINE` 휘발유
- `DIESEL` 디젤
- `LPG` LPG (선택 지원)
- `HEV_GASOLINE` 휘발유 하이브리드
- `HEV_DIESEL` 디젤 하이브리드 (희귀/적용 시)
- `PHEV_GASOLINE` 플러그인 하이브리드
- `PHEV_DIESEL` 플러그인 디젤 하이브리드 (적용 시)
- `BEV` 순수전기
- `OTHER` 기타/미확인

**차량 차체/용도(`vehicleClass`)**
- `SEDAN`, `SUV`, `VAN`, `PICKUP`, `LIGHT_TRUCK`, `MEDIUM_TRUCK`, `HEAVY_TRUCK`, `BUS`, `OTHER`.

트럭은 연료 유형이 아니다. 예: `HEAVY_TRUCK + DIESEL`, `LIGHT_TRUCK + BEV`, `PICKUP + GASOLINE` 모두 가능해야 한다.

**기본 정보**
- vehicleId(UUID), 별명, 제조사, 모델, 세대/트림, 모델연도, 최초등록일, 차량번호(선택), 차량식별번호 VIN(선택/민감정보), 배기량, 엔진코드, 변속기(`AT`, `MT`, `CVT`, `DCT`, `EV_SINGLE`, `OTHER`), 2WD/4WD/AWD, 배터리/냉각수 구성(차량 지원 시), 현재 주행거리, 주행거리 갱신일.
- 시장/판매국가(KR 등), 사용자 유형(자가용/업무용/택시/배송/운송), 정상/가혹조건 설정.
- 트럭 전용: 최대적재량(선택), 차량총중량 등급(선택), 에어브레이크 유무, 후처리 장치(`DPF`, `SCR`), 연료필터 수, 엔진가동시간 계기 지원 여부, PTO 유무.

**운행조건 체크** (복수 선택)
- 짧은 거리 반복, 장시간 공회전, 정체/정지 출발 반복, 먼지 많은 도로, 산악·급경사, 혹한/혹서, 견인/상시 고하중, 영업용·장시간 운행, 물·염화칼슘 노출, 비포장도로, 고속·급가감속.
- 자동으로 고정된 '0.5배' 교체 규칙을 적용하지 않는다. 가혹조건용 **제조사 별도 테이블**이 있으면 해당 테이블을 사용한다. 없으면 '가혹조건 주기 미확인' 배지를 표시하고 사용자 확인을 요청한다.

---

## 3. 소모품 카탈로그 / 적용 조건

각 소모품 정의 필드:
`itemCode`, `displayNameKo`, `category`, `actionType(INSPECT/REPLACE/REFILL/ROTATE/CLEAN)`, `applicabilityExpression`, `intervalKm?`, `intervalMonths?`, `intervalEngineHours?`, `conditionTriggers[]`, `initialIntervalKm?`, `repeatIntervalKm?`, `initialIntervalMonths?`, `repeatIntervalMonths?`, `sourceType`, `sourceUrl?`, `sourceModelYear?`, `notes`, `criticality`, `enabled`.

`—`는 교환주기 없음/확인 필요를 의미한다. 서로 다른 차종에 대해 수치를 임의 복사하지 않는다.

### 3.1 승용 공통 기본 템플릿 (초기 참고값)

| 코드 | 항목 | 초기 추천 관리 방식 | 샘플 알림/주기(참고, OEM 우선) | 비고 |
|---|---|---|---|---|
| CABIN_FILTER | 실내/에어컨 필터 | REPLACE | 10,000~20,000 km 또는 6~12개월 | 먼지·악취 시 조기 점검 |
| TIRES | 타이어 트레드/손상 | INSPECT | 1개월 또는 주행거리 갱신 때 | 법규 최소 마모한계, 계절, 편마모/손상 별도 |
| TIRE_ROTATION | 타이어 위치교환 | ROTATE | 8,000~12,000 km 참고 | 구동방식·타이어 규격/방향성 제한 |
| TIRE_PRESSURE | 타이어 공기압 | INSPECT | 매월 | 운행 전 냉간 측정 권장 |
| BRAKE_PAD | 브레이크 패드 | INSPECT | 10,000~20,000 km 또는 12개월 샘플 | 고정 교체 km 제공 금지, 잔량 기록 |
| BRAKE_DISC | 브레이크 디스크 | INSPECT | 패드 교환·진동 발생 시 | 두께/런아웃/균열 점검, 부품 한계치 OEM |
| BRAKE_FLUID | 브레이크액 | INSPECT + REPLACE | 초기 후보 24개월 교체, 제조사 확인 | 일부 OEM은 48개월 등 상이 |
| WIPER | 와이퍼 블레이드 | REPLACE 또는 상태 점검 | 6~12개월 후보 | 닦임 불량 시 즉시 교체 |
| WASHER_FLUID | 워셔액 | REFILL | 잔량 부족 시 | 정기 교체 대상 아님 |
| BATTERY_12V | 12V 배터리 | INSPECT | 매년/성능 저하 시 | 수명 연수로 무조건 교환하지 않음 |
| ALIGNMENT | 휠 얼라인먼트 | INSPECT | 차량 쏠림·편마모·충격 시 | 정기 무조건 수행 아님 |
| SUSPENSION | 조향/현가/부싱 | INSPECT | 정기 점검 또는 이상 소음 시 | 마모/누유/유격 |
| AC_SYSTEM | 에어컨 냉매/컴프레서 | INSPECT | 냉방성능 저하 또는 OEM 기준 | 냉매 무조건 정기 충전 금지 |
| SAFETY_LIGHTS | 전조등/제동등 등 | INSPECT | 매월 | 고장 시 즉시 수리 |
| DRIVE_SHAFT | 등속조인트/부트 | INSPECT | 정기 점검 | 찢어짐·그리스 누출 |

### 3.2 휘발유 `GASOLINE`

| 항목 | 초기 참고 관리 기준 | 적용 유의점 |
|---|---|---|
| 엔진오일+오일필터 | 10,000 km 또는 12개월 **샘플값** | 터보/자연흡기/오일 규격/가혹조건으로 변경, 함께 처리 가능 |
| 엔진 에어필터 | 15,000 km마다 점검, 약 30,000~45,000 km 교체 후보 | 먼지/매뉴얼 기준 |
| 점화플러그 | 차종별 40,000~160,000 km 수준에서 크게 상이, **기본 확정 알림 비활성** | 플러그 재질·엔진별 차이 큼 |
| 점화코일 | 증상/진단에 따라 | 정기 무조건 교체 대상 아님 |
| 구동벨트 | 균열·마모·장력 점검 | 타이밍벨트와 별도 관리 |
| 타이밍벨트/체인 | 차량 장착 여부에 따라 개별 등록 | 체인에 획일적 교체주기 부여 금지 |
| 냉각수 | OEM 최초/반복 교환주기 조회 필수 | 장수명 냉각수는 10년 수준 제품도 있음 |
| 연료필터 | `OEM_REQUIRED` | 탱크 일체형/무교환형 및 교체형 구분 |
| 변속기오일 | 변속기 종류 및 OEM 기반 | AT/CVT/DCT/MT 각각 다름 |

### 3.3 디젤 `DIESEL`

휘발유 공통 엔진·구동계 항목에서 점화플러그를 **비적용**하고 아래 항목을 추가한다.

| 항목 | 초기 참고 관리 기준 | 적용 유의점 |
|---|---|---|
| 디젤 엔진오일+필터 | 10,000 km 또는 12개월 샘플 | DPF 대응 규격, 상용차는 엔진시간/연료 사용량 기준 가능 |
| 연료필터 | **20,000~40,000 km 범위는 탐색용 후보, 기본 확정 알림 비활성** | 고압 연료계통 보호, OEM 주기 확정 필요 |
| 연료 수분분리기/드레인 | 장착 시 상태 점검 및 경고 시 조치 | 필터/워터세퍼레이터 여부 확인 |
| DPF | 경고등/재생 실패·차압·진단 결과에 따라 점검 | 일정 km마다 무조건 교체·강제재생 금지 |
| SCR/요소수(AdBlue/DEF) | 부족 경고 및 잔량 기반 보충 | 소모량은 운행에 따라 달라짐 |
| EGR/흡기 카본 | 이상 증상/정비지침 기반 | 무조건 세척 주기 부여 금지 |
| 예열플러그 | 고장 진단 시 교체 | 가솔린 점화플러그와 혼동 금지 |
| 터보차저/흡기호스 | 누유/누설/출력 이상 점검 | 고정 교체주기 없음 |

### 3.4 일반 하이브리드 `HEV_*` 및 플러그인 `PHEV_*`

- HEV/PHEV는 전기모터가 있어도 **내연기관 오일, 필터, 점화플러그/디젤 연료필터 등**이 해당 차량에 적용된다.
- `HEV_GASOLINE`: GASOLINE 템플릿 + 다음 추가 항목.
- `HEV_DIESEL`: DIESEL 템플릿 + 다음 추가 항목.
- `PHEV_*`: HEV 계열 + 충전 포트/케이블 상태 점검.

| 항목 | 초기 관리 정책 | 유의점 |
|---|---|---|
| 엔진오일 | 제조사 운행조건별 주기 | 엔진 가동시간이 적다고 사용기간 기준 무시 금지 |
| HSG 벨트/하이브리드 구동벨트 | **장착 차량만** 점검/교체 | 일부 현대차 매뉴얼에는 예: 90,000 km/48개월 교체, 다른 모델에는 다른 값 |
| 엔진 클러치 액추에이터 오일/작동유 | **장착 차량만** | 일부 매뉴얼의 30,000 km/24개월 교체 사례는 일반화 금지 |
| 하이브리드 모터/인버터 냉각수 | OEM 최초/반복 주기 별도 | 엔진 냉각수와 회로가 분리될 수 있음 |
| 고전압 배터리 냉각 필터/흡입구 | 장착 방식에 따라 점검/청소 | 고전압 배터리 분해 작업 지시 금지 |
| 회생제동 포함 제동장치 | 패드 두께/부식/작동 점검 | 회생제동으로 패드 마모가 적어도 점검 필요 |
| 12V 배터리 | 구성 확인 후 상태 점검 | 별도 12V 배터리 또는 통합 방식 확인 |

### 3.5 전기차 `BEV`

전기차에서는 엔진오일, 엔진오일 필터, 엔진 흡기필터, 점화플러그, DPF, 요소수, 엔진 타이밍벨트를 **자동 제외**한다.

| 항목 | 초기 관리 정책 | 유의점 |
|---|---|---|
| 감속기 오일 | OEM 지정 시 `INSPECT`, `REPLACE` 구분 | 고정 '무조건 교체' 설정 금지 |
| 구동모터/인버터·고전압 배터리 냉각수 | OEM 최초/반복 정비표 필수 | 일부 차량·연식은 별도 저전도도 냉각수와 교환 절차 필요 |
| 배터리 냉각계 필터 | 장착 여부 확인 | 모델별 차이 큼 |
| 브레이크액 | OEM 교체 주기 | 회생제동 차량도 필요 |
| 브레이크 패드·디스크 | 상태·부식 점검 | 패드 마모가 적어도 디스크 부식 가능 |
| 타이어 | 마모·편마모·하중지수 점검 | 차량 중량/토크, EV 규격 확인 |
| 캐빈필터 | 공통 기준 또는 OEM | HEPA/특수필터 장착 여부 |
| 12V 보조배터리 | 상태 점검 | 12V/저전압 구성 구분 |
| 충전포트/케이블 | 육안/상태 점검 | 커넥터 과열·파손 시 전문점 점검 |
| 고전압 배터리 SOH | 앱이 값 수집 가능한 경우 참고 지표 | SOH 자체를 임의 교체주기로 사용 금지 |

### 3.6 트럭 `*_TRUCK` (연료별 템플릿 위에 추가하는 **오버레이**)

**트럭은 동력 유형에 덮어쓰는 추가 정책이다.** 경유 트럭은 디젤+트럭, 전기 트럭은 BEV+트럭.

| 항목 | 조건 | 관리 방식 |
|---|---|---|
| 엔진오일/필터 | 내연기관 트럭 | 거리/기간/엔진가동시간 등 OEM 기준 중 최초 도래값 |
| 연료필터 1차/2차·수분분리기 | 디젤/장착 시 | 각 필터 개별 항목, 수분 경고에 따른 정비 |
| DPF/SCR/DEF | 디젤 후처리 장착 시 | 재생 상태, 경고, 요소수 잔량, OEM 정비지침 |
| 에어드라이어 카트리지 | 에어브레이크 장착 시 | 수분관리 및 제조사 정비표 |
| 에어탱크/공압 누설 | 에어브레이크 장착 시 | 운행 전 점검/법정 점검 항목과 구분 |
| 브레이크 라이닝/드럼·에어라인 | 차량 사양에 따라 | 마모·두께·누설·작동 점검 |
| 구리스 주입 | 그리스 니플 장착 차축·샤시 등 | 위치별 거리/엔진시간/OEM 간격, 무급유 부품은 제외 |
| 디퍼렌셜/차동기어 오일 | 구동축별 | 차축 규격/견인·고하중 조건/최초·반복 주기 |
| 트랜스퍼 케이스 오일 | 4WD/AWD 적용 시 | 별도 주기 |
| 휠베어링/허브 오일·그리스 | 서비스 가능 구조일 때 | 밀봉형 베어링 정기 분해·그리스 주입 금지 |
| 타이어 위치교환/공기압 | 복륜/축 배열별 | 단순 승용차 로테이션 패턴 사용 금지 |
| 판스프링/서스펜션/킹핀 | 해당 구성 시 | 점검/급유 이력 |
| PTO/유압유·필터 | PTO/특장 장비 장착 시 | 차량과 상부장비를 분리한 시간·주기 |
| 트레일러 연결장치 | 견인 적용 시 | 핀·커플링·잠금·급유 등 점검 |
| 냉각계통/호스 | 내연기관·EV별 | 개별 OEM 기준 |

> 대형 상용차/특장차는 차대 제조사, 엔진 제조사, 변속기/차축/특장 장비 제조사의 정비지침이 서로 다를 수 있다. 설정 화면에 '정비기준 출처'를 여러 개 연결할 수 있게 한다.

---

## 4. OEM 교체주기 프로파일 시스템 (핵심 설계)

### 4.1 우선순위
1. **차량별 사용자 직접 설정**(`USER_OVERRIDE`) — 사용자 선택을 유지하고 출처 표시.
2. **완전 일치 OEM 프로파일**(`VERIFIED_OEM`) — 시장/제조사/모델연도/엔진/파워트레인/변속기/구동계/옵션을 검증.
3. **차량군 OEM 프로파일**(`OEM_FAMILY`) — 동일 엔진·플랫폼 적용 근거 존재 시, 확인 필요 배지 표시.
4. **차량 유형 기본 템플릿**(`GENERIC_ESTIMATE`) — 샘플값 또는 점검 권고, '제조사 기준 미확인' 표시.
5. **장착 여부가 불명확한 항목**(`NOT_CONFIGURED`) — 알림을 만들지 않고 확인 요청.

차종을 선택했다고 임의 수치를 '제조사 권장'으로 표현하지 않는다.

### 4.2 정상/가혹조건
- `NORMAL`과 `SEVERE`를 OEM 자료의 개별 정비표로 각각 관리.
- 심한 운행조건이 선택됐으나 대응 표가 없는 경우 `SEVERE_UNVERIFIED` 표시. **일괄 50% 단축 금지.**
- 일부 제조사에서 오일수명 모니터(Oil Life Monitor)를 제공한다면, 사용자가 별도로 계기판 표시를 입력할 수 있도록 설계; 앱 예측과 혼동 금지.
- 트럭은 주행거리 외 `engineHours` 및 `calendarMonths`를 지원하고, OEM이 인정한 기준만 활성화한다.

### 4.3 최초/반복 및 이벤트 모델
- 냉각수처럼 '최초 100,000km/120개월, 이후 20,000km/24개월' 구조는 `firstServiceRule`, `repeatServiceRule` 분리.
- `INSPECT`와 `REPLACE`는 독립 작업유형. 점검 완료는 마지막 교체시점을 초기화하지 않는다.
- 정비 기록에 `actionType`, `odometerKm`, `engineHours?`, `serviceDate`, `partCode`, `quantity`, `notes`, `workshop`, `cost`, `evidenceUri[]` 필수/선택 필드 정의.
- 서로 다른 부품을 한 번에 교체하면 `serviceVisit` 하나 아래 `serviceLine` 여러 개 저장.
- 안전부품은 `conditionBased = true` 및 마모량(mm)·상태·다음 점검일을 기록. 교체 수명 예측은 별도 모델로 취급.

## 5. 다음 교체시기 및 알림 엔진

### 5.1 주요 계산
- 일반 반복 주기: `nextDueKm = lastReplacementKm + intervalKm`.
- 날짜: `nextDueDate = lastReplacementDate.plusMonths(intervalMonths)`.
- 엔진 가동시간: `nextDueHours = lastReplacementHours + intervalEngineHours`.
- 최초 주기: `firstServiceRule`에 정의된 **기준점**(출고/최초등록/차량 누적 주행거리)을 사용. 정확한 기준점 없으면 확인 요청.
- **발동 판단은 활성화된 기준의 OR**: 현재 주행거리 >= 교체 km **또는** 오늘 >= 교체일 **또는** 가동시간 >= 교체 시간.
- 하나의 기준만 초과하면 `OVERDUE`. 주행거리/날짜/시간 기준 중 하나라도 근접하면 `DUE_SOON`.
- 마모·상태 기준 항목은 별도 `INSPECTION_DUE`, `ISSUE_REPORTED`로 표시.

### 5.2 알림 기본 정책 (설정 가능)
- 거리 알림: 예정 1,000 km 전, 500 km 전, 도래 시, 초과 시.
- 날짜 알림: 예정 30일 전, 7일 전, 당일, 초과 후 7일마다(사용자가 허용한 경우).
- 엔진시간 알림: 예정 20시간 전(시간 기준 활성 차량만).
- 마지막 주행거리 갱신일이 오래되면 '주행거리 입력 요청'만 알림, 수치를 사실처럼 추정하여 `OVERDUE` 판정하지 않는다.
- 월평균 주행거리로 계산한 교체 날짜는 **예상**이며 확정 날짜와 구별.
- 여러 차량 알림은 일괄 요약 또는 차량별 설정; 시간대/방해금지 고려.

### 5.3 알림 우선순위
`OVERDUE` > `DUE_NOW` > `DUE_SOON` > `INSPECTION_DUE` > `NORMAL`; `NOT_CONFIGURED`와 `SEVERE_UNVERIFIED`는 별도의 '설정 확인' 섹션.

### 5.4 Kotlin 의사코드
```kotlin
data class IntervalRule(val km: Int?, val months: Int?, val engineHours: Int?)
data class DueResult(val isOverdue: Boolean, val isSoon: Boolean)

fun evaluateDue(
    now: LocalDate, currentKm: Int, currentHours: Int?,
    lastDate: LocalDate, lastKm: Int, lastHours: Int?,
    rule: IntervalRule, kmWarning: Int = 1000, daysWarning: Long = 30
): DueResult {
    val kmDue = rule.km?.let { lastKm + it }
    val dateDue = rule.months?.let { lastDate.plusMonths(it.toLong()) }
    val hourDue = if (rule.engineHours != null && lastHours != null && currentHours != null)
        lastHours + rule.engineHours else null

    val overdue = (kmDue != null && currentKm >= kmDue) ||
        (dateDue != null && !now.isBefore(dateDue)) ||
        (hourDue != null && currentHours!! >= hourDue)
    val soon = (kmDue != null && currentKm >= kmDue - kmWarning) ||
        (dateDue != null && !now.isBefore(dateDue.minusDays(daysWarning))) ||
        (hourDue != null && currentHours!! >= hourDue - 20)
    return DueResult(isOverdue = overdue, isSoon = !overdue && soon)
}
```
> 구현 시 음수 주기·미래 정비일·주행거리 역행·누락 기준·타임존·중복 알림을 검증한다. 시간 기준을 활성화했는데 값이 없으면 계산하지 말고 `NEEDS_METER_READING` 표시.

---

## 6. 화면 및 사용자 흐름

1. **온보딩:** 앱 목표 → 차량 추가 → 제조사/차종/연식/연료/차체 → 주행거리 → 사용환경 → 적용 소모품 확인.
2. **통합 대시보드:** 전체 등록 차량 카드, '초과 N / 곧 도래 N / 점검 N', 가까운 정비예정일, 주행거리 입력 바로가기.
3. **차량 상세:** 사진/계기판, 소모품 현황(상태 배지, 남은 km/일), 정비기록, 차량 설정.
4. **소모품 상세:** 품목 설명, 적용 이유, 제조사/사용자/일반 템플릿 출처, 마지막 작업, 다음 예정 km/날짜, 정상/가혹 주기, 알림 설정.
5. **정비기록 입력:** 날짜/누적 km 입력 → 품목 다중 선택 → 점검/교체/보충 작업 → 부품·금액·정비소·사진 → 저장 시 해당 품목만 재계산.
6. **알림함:** 차량별/품목별 필터, 알림 해제·일시중지·정비 기록으로 이동. '알림 삭제'만으로 교체 완료 처리 금지.
7. **정비기준 검증:** OEM 출처 연결/주기 편집/장착 여부 확인/가혹조건 경고.
8. **내역·통계:** 월·연도별 차량 유지비, 품목별 비용, 정비 이력 내보내기.

하단 내비게이션 예: `홈 | 내 차량 | 정비기록 | 알림 | 설정`.

## 7. 데이터베이스 설계 (Room)

| 테이블 | 주요 컬럼 | 설명 |
|---|---|---|
| `vehicles` | id, nickname, make, model, modelYear, powertrain, vehicleClass, engineCode, transmission, drivetrain, odometerKm, updatedAt | 1:N 차량 관리 |
| `vehicleUsageProfiles` | vehicleId, severity, useCasesJson, yearlyKmEstimate, engineHours | 운행조건 |
| `maintenanceCatalog` | itemCode, label, actionTypes, conditionalApplicability, severity | 소모품 정의 |
| `oemProfiles` | id, market, make, model, yearFrom, yearTo, engineCode, powertrain, drivetrain, sourceUrl, verifiedAt, version | 제조사 기준 식별 |
| `maintenanceRules` | id, vehicleId, itemCode, actionType, intervalKm, months, hours, firstRepeatMode, sourceType, profileId, verified, enabled | 차량별 적용 규칙 |
| `serviceVisits` | id, vehicleId, date, odometerKm, workshop, totalCost, notes | 한 번의 정비 방문 |
| `serviceLines` | id, visitId, itemCode, actionType, partsCost, laborCost, conditionValue, notes | 작업 단위 |
| `meterReadings` | id, vehicleId, date, odometerKm, engineHours, inputSource | 거리/시간 이력 |
| `notifications` | id, vehicleId, itemCode, actionType, ruleId, dueKey, sentAt, status | 중복방지 및 발송 이력 |
| `attachments` | id, visitId, uri, mimeType | 영수증/사진 URI |

- Room 마이그레이션으로 기존 정비기록 보존.
- 모든 레코드는 UUID; 차량 삭제는 기본적으로 `archived` 소프트삭제.
- `serviceLines`의 `INSPECT`는 `REPLACE` 기준점에 영향 없음.
- DB는 `sourceType`, `sourceUrl`, `verifiedAt`과 정비주기 `revision`을 보존해 기준 변경을 추적.
- 사진은 Android Photo Picker 및 앱 전용 저장소/안전한 URI 권한 사용.

## 8. 기술 스택 및 프로젝트 구조

- 언어/환경: Kotlin, Android Studio, Jetpack Compose + Material 3.
- 아키텍처: MVVM + Repository + UseCase, 단방향 UI State.
- 로컬 데이터: Room, 앱 설정: DataStore.
- DI: Hilt (선택; 팀 규모에 맞춰 사용).
- 백그라운드: WorkManager로 주기적 검토, 정확한 시각 보장 불가. 앱 실행/주행거리 수정/정비 저장 때 즉시 재평가.
- 알림: NotificationChannel, Android 13+ `POST_NOTIFICATIONS` 런타임 권한, 앱 내 알림함은 권한 거절해도 작동.
- 시간: `java.time.LocalDate`, 사용자 로컬 타임존.
- Android 최신 target SDK, 라이브러리 버전은 프로젝트 착수 시 공식 개발자 문서에 따라 확정.

```text
app/
  data/
    local/          # Room entities, DAO, migrations
    repository/     # Vehicle, rule, service, notification repositories
    seed/           # generic catalog and OEM profile JSON
  domain/
    model/
    rules/          # ApplicabilityResolver, MaintenanceScheduleEngine
    usecase/        # RegisterVehicle, LogService, EvaluateDue, UpdateOdometer
  ui/
    dashboard/
    vehicles/
    maintenance/
    servicehistory/
    notifications/
    settings/
  worker/
    MaintenanceNotificationWorker.kt
```

## 9. JSON 템플릿 예시

```json
{
  "schemaVersion": 1,
  "profileId": "generic-bev-v1",
  "match": {"powertrain": "BEV", "vehicleClass": "ANY"},
  "sourceType": "GENERIC_ESTIMATE",
  "rules": [
    {
      "itemCode": "CABIN_FILTER",
      "actionType": "REPLACE",
      "intervalKm": 15000,
      "intervalMonths": 12,
      "status": "USER_CONFIRM_REQUIRED"
    },
    {
      "itemCode": "REDUCTION_GEAR_OIL",
      "actionType": "INSPECT",
      "intervalKm": null,
      "intervalMonths": null,
      "status": "OEM_REQUIRED"
    },
    {
      "itemCode": "EV_COOLANT",
      "actionType": "REPLACE",
      "intervalKm": null,
      "intervalMonths": null,
      "firstServiceRule": null,
      "repeatServiceRule": null,
      "status": "OEM_REQUIRED"
    }
  ]
}
```

### 적용성 결정 로직
1. `powertrain`으로 ICE/HEV/PHEV/BEV 기본 카탈로그 결정.
2. `vehicleClass`가 트럭/버스면 샤시·상용차 오버레이 병합.
3. 사양(`engineCode`, 변속기, 4WD, SCR, 공압브레이크, PTO 등)으로 조건부 품목 적용.
4. 매칭 OEM 프로파일의 점검/교체주기로 기본값 재정의.
5. 운행조건 정상/가혹 프로파일 적용(존재 시).
6. 사용자 개별 설정 반영.
7. 각 항목에 `ruleSource` 및 신뢰상태 표시; 미확정값은 확정 제조사 알림으로 표시하지 않음.

## 10. 중요한 예외사항 및 검증 테스트

- 전기차 등록 → 엔진오일·점화플러그·DPF 미표시.
- 디젤 등록 → 점화플러그 대신 연료필터/DPF/SCR(장착조건) 후보.
- 디젤 트럭 등록 → 승용 디젤 항목 + 차축/공압/그리스/PTO(장착 시).
- 전기 트럭 등록 → BEV 항목 + 트럭 오버레이; 내연기관 오일은 생성 금지.
- 하이브리드 등록 → 엔진오일 필수 후보, HSG 벨트/클러치 액추에이터는 모델별 조건부.
- 제조사 주기 값이 없는 항목 → 주기 미확인, 알림일 임의 생성 금지.
- '최초 100,000km/120개월 → 반복 20,000km/24개월' → 첫 교체 전/후 계산 분리.
- 날짜만 만기·거리 미도래 → 예정/초과 상태 정확히 표시.
- 점검 기록만 저장 → 교환주기 기준점 미변경.
- 동일 정비 방문에서 여러 품목 교체 → 각각 다음 주기 갱신.
- 같은 차량의 과거보다 작은 주행거리 입력 → 사용자 재확인 및 수정 로그.
- 알림 권한 거부 → 앱 내 상태는 정확히 유지.
- 차량 A 정비 → 차량 B의 알림·이력 영향 없음.
- OEM 템플릿 업데이트 → 사용자 오버라이드는 덮어쓰지 않음, 변경 내역 확인 제공.
- 가혹조건 체크 → OEM 가혹조건 데이터가 없으면 임의 주기 절반 단축 금지.

## 11. 개발 단계 및 완료 기준

### 1단계 — 핵심 데이터/엔진
- 차량 등록, 다중 차량, 공통/연료별/트럭 오버레이 카탈로그, 규칙 출처, Room.
- 거리/날짜/시간 기반 판단 및 최초/반복 주기 단위 테스트.

### 2단계 — UI/정비 이력
- Compose 대시보드, 차량 상세, 소모품 설정, 정비 다중입력, 사진/영수증 연결.

### 3단계 — 알림 및 안정성
- WorkManager, NotificationChannel, 권한 처리, 알림 중복 방지, 백업/복원.

### 4단계 — 정비표 정확도 확대
- 공식 매뉴얼 근거가 확인된 차량별 OEM 프로파일 누적.
- 모델연도 및 엔진별 다른 교환주기, 가혹조건, 최초/반복 수명 검증.

**출시 통과 조건:** 차량 5대 이상 독립 관리, 동력별 부적합 품목 차단, 정비저장 즉시 일정 재산정, 오프라인 동작, 알림 중복 방지, 출처별 상태 표시, 단위 테스트 통과.

## 12. 제조사 정비표 참고 출처 / 프로파일 구축 원칙

다음 공식 자료는 **차종별 주기가 일치하지 않는다는 근거 및 OEM 프로파일 구축 사례**로만 사용한다. 특정 출처의 수치를 다른 연식·국가·모델에 복제하지 않는다.

1. 현대자동차: 정상 vs 가혹조건 안내 — https://ownersmanual.hyundai.com/docview/webhelp/Hyundai/0e169f84-6c7e-4286-a068-f3dbfd5bbb12/ide5b10be92ba.html
2. 현대자동차 2025 DN8(가솔린) 정상 정비표 — https://ownersmanual.hyundai.com/full_webhelp/DN8/2025/en_US/topic_ybb_1yh_lwb.html
3. 현대자동차 2025 DN8HEV 정상 정비표 — https://ownersmanual.hyundai.com/full_webhelp/DN8HEV/2025/en_US/topic_ybb_1yh_lwb.html
4. 현대자동차 HEV 정비표(HSG, 액추에이터 포함 사례) — https://ownersmanual.hyundai.com/docview/webhelp/Hyundai/5375bf28-c9d4-4116-920b-b92a819711e6/topic_vzl_y5g_zwb.html
5. 기아 EV 정비표(냉각수, 감속기 오일 등) — https://ownersmanual.kia.com/docview/webhelp/doc/5b7dde3a-285c-44b3-9e31-b1b744dd25e8/topics/chapter9_4.html
6. 기아 EV6 정비표(연식/시장별 차이 존재) — https://www.kia.com/content/dam/kia2/in/en/content/ev6-manual/topics/chapter8_4.html
7. Ford 상용 디젤 정비 사례 — https://www.fordservicecontent.com/Ford_Content/vdirsnet/OwnerManual/Home/Content?ProcUid=G2426419&Uid=G2426418&buildtype=web&countryCode=USA&div=f&languageCode=en&userMarket=PRI&vFilteringEnabled=False&variantid=10789

### 다음 버전에서 권장하는 데이터 운영방식
- 제조사/모델/연식/엔진/시장 단위 정비표를 JSON 데이터팩으로 별도 버전관리.
- 각 규칙에 `sourceDocument`, `sourceSection`, `effectiveModelYears`, `reviewedBy`, `reviewedAt`, `confidence` 포함.
- 동일 모델명도 연식/국가가 다르면 프로파일 재검토.
- '일반 추천'과 '제조사 확인'을 색상뿐 아니라 명확한 텍스트로 구분.
- 위험도가 높은 브레이크·타이어·고전압 관련 사항은 '정비 점검 필요'로 안내하며 임의 안전판정 금지.
