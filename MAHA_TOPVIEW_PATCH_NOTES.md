# MAHA Top View Sketch Patch

이번 패치는 사용자가 요청한 방향대로 상세 페이지의 차량 상태 점검도를 수정하기 위한 버전입니다.

## 핵심 변경
- 기존 도식형 캔버스 차량 그림 대신, 실제 차량에 가까운 **탑뷰 라인아트 이미지** 적용
- 부품 상태 박스를 차량 외곽에 배치
- 각 박스에서 차량 해당 위치로 **리더 라인(연결선)** 표시
- 겹침을 줄이기 위해 좌/우/하단으로 박스를 재배치

## 수정 파일
- `app/src/main/java/maha/garage/ui/components/VehicleSketchIllustrations.kt`
- `app/src/main/res/drawable/car_top_lineart.png`
