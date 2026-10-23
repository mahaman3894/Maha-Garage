# MAHA Garage line / anchor refinement

## 이번 수정 내용
- 차량 상태 점검도의 콜아웃 선을 박스 중앙이 아니라 **박스 가장자리**에서 시작하도록 수정
- 상단/하단/좌측/우측 박스 방향별로 선 시작점을 다르게 계산
- 차량 쪽 연결점에 **흰색 + 시안색 포인트 점**을 다시 추가
- 선에 은은한 glow(후광) 레이어를 추가해 가독성 개선
- 박스 위치 및 연결점 좌표를 사용자가 마지막으로 승인한 시안 비율에 맞게 재조정

## 주요 수정 파일
- app/src/main/java/maha/garage/ui/components/VehicleSketchIllustrations.kt
