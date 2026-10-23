# MAHA Garage Sketch Update Notes

이번 수정은 `VehicleSketchIllustrations.kt` 중심으로 진행되었습니다.

## 변경 내용
- 내 차량 카드용 차량 스케치를 더 자연스러운 사이드 뷰로 개선
- 차량 상세 페이지의 `차량 부품 위치별 상태 점검도`를 실차 느낌의 탑뷰 블루프린트 스타일로 재구성
- 차체 실루엣, 보닛, 윈드실드, 루프, 도어 라인, 휠/타이어, 미러, 전후등 표현 강화
- 소모품 상태 박스를 차량 주변 콜아웃 형태로 재배치
- 소모품 박스와 실제 차체 위치를 연결하는 리더 라인 추가
- 전기차(BEV)에서는 엔진오일 대신 감속기오일/EV 냉각수 로직 분기 반영

## 주요 수정 파일
- `app/src/main/java/maha/garage/ui/components/VehicleSketchIllustrations.kt`
