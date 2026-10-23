package maha.garage

import maha.garage.util.ReceiptParser
import org.junit.Assert.*
import org.junit.Test

class ReceiptParserTest {

    @Test
    fun testStandardGsReceipt() {
        val raw = """
            GS칼텍스 삼거리주유소
            대표자: 홍길동
            [매출전표]
            품명: 휘발유
            단가: 1,685원
            수량: 42.15L
            결제금액: 71,023
            승인번호: 12345678
        """.trimIndent()

        val parsed = ReceiptParser.parse(raw)
        assertEquals("GS칼텍스 삼거리주유소", parsed.gasStationName)
        assertEquals(42.15, parsed.fuelAmount ?: 0.0, 0.01)
        assertEquals(1685, parsed.unitPrice)
        assertEquals(71023, parsed.totalPrice)
    }

    @Test
    fun testSkReceiptWithLiterSymbol() {
        val raw = """
            SK에너지 강남셀프주유소
            단 가 : 1,650
            주유량 : 30.00ℓ
            합계금액 : 49,500원
        """.trimIndent()

        val parsed = ReceiptParser.parse(raw)
        assertTrue(parsed.gasStationName?.contains("SK에너지") == true)
        assertEquals(30.0, parsed.fuelAmount ?: 0.0, 0.01)
        assertEquals(1650, parsed.unitPrice)
        assertEquals(49500, parsed.totalPrice)
    }

    @Test
    fun testSoilReceiptWithDerivedTotal() {
        val raw = """
            S-OIL 행복만점주유소
            수량: 50.00리터
            단가: 1,600
        """.trimIndent()

        val parsed = ReceiptParser.parse(raw)
        assertEquals(50.0, parsed.fuelAmount ?: 0.0, 0.01)
        assertEquals(1600, parsed.unitPrice)
        assertEquals(80000, parsed.totalPrice)
    }

    @Test
    fun testDerivedLiterFromTotalAndUnit() {
        val raw = """
            현대오일뱅크 직영점
            단가: 1,700
            결제금액: 85,000
        """.trimIndent()

        val parsed = ReceiptParser.parse(raw)
        assertEquals(85000, parsed.totalPrice)
        assertEquals(1700, parsed.unitPrice)
        assertEquals(50.0, parsed.fuelAmount ?: 0.0, 0.01)
    }

    @Test
    fun testTableRowReceiptWithoutColons() {
        // 테이블 형태로 단가, 수량, 금액이 한 줄에 출력된 가장 흔한 영수증
        val raw = """
            SK엔크린 판교주유소
            [주유내역]
            품명  단가  수량  금액
            휘발유 1,650 30.30 50,000
            카드승인번호: 98765432
        """.trimIndent()

        val parsed = ReceiptParser.parse(raw)
        assertTrue(parsed.gasStationName?.contains("SK엔크린") == true)
        assertEquals(30.30, parsed.fuelAmount ?: 0.0, 0.01)
        assertEquals(1650, parsed.unitPrice)
        assertEquals(50000, parsed.totalPrice)
    }

    @Test
    fun testThreeDecimalsQuantityWithoutL() {
        // 소수점 3자리 및 L 표시 없는 계량 영수증
        val raw = """
            HD현대오일뱅크 직영셀프주유소
            단가 1,689
            수량 41.445
            금액 70,000
        """.trimIndent()

        val parsed = ReceiptParser.parse(raw)
        assertTrue(parsed.gasStationName?.contains("HD현대오일뱅크") == true)
        assertEquals(41.45, parsed.fuelAmount ?: 0.0, 0.05)
        assertEquals(1689, parsed.unitPrice)
        assertEquals(70000, parsed.totalPrice)
    }

    @Test
    fun testScatteredNumbersWithTriangulation() {
        // 라벨이 없거나 불완전해도 도메인 범위와 곱셈 검증으로 찾아내는 케이스
        val raw = """
            ex-OIL 만남의광장주유소
            TEL: 02-123-4567
            사업자번호: 123-45-67890
            1,620
            37.04
            60,000
            승인일시: 2026-10-24 10:20:30
        """.trimIndent()

        val parsed = ReceiptParser.parse(raw)
        assertTrue(parsed.gasStationName?.contains("ex-OIL") == true)
        assertEquals(37.04, parsed.fuelAmount ?: 0.0, 0.05)
        assertEquals(1620, parsed.unitPrice)
        assertEquals(60000, parsed.totalPrice)
    }

    @Test
    fun testSoilVariants() {
        // 에쓰오일 상호 및 영수증 파싱 검증
        val raw1 = """
            상호 : 에쓰오일(주) 구도일주유소 역삼점
            휘발유 단가 1,690원
            수량 35.50L
            결제금액 59,995원
        """.trimIndent()
        val parsed1 = ReceiptParser.parse(raw1)
        assertTrue(parsed1.gasStationName?.contains("에쓰오일") == true)
        assertEquals(35.50, parsed1.fuelAmount ?: 0.0, 0.01)
        assertEquals(1690, parsed1.unitPrice)
        assertEquals(59995, parsed1.totalPrice)

        // S-Oil 영수증
        val raw2 = """
            [영수증] S-Oil 강남셀프주유소 02-555-1234
            단가: 1640
            수량: 45.00
            승인금액: 73,800
        """.trimIndent()
        val parsed2 = ReceiptParser.parse(raw2)
        assertTrue(parsed2.gasStationName?.contains("S-Oil") == true)
        assertEquals(45.0, parsed2.fuelAmount ?: 0.0, 0.01)
        assertEquals(1640, parsed2.unitPrice)
        assertEquals(73800, parsed2.totalPrice)
    }

    @Test
    fun testAlteulGasStation() {
        val raw = """
            가맹점명: 알뜰주유소 평화셀프점
            품명: 경유
            단가: 1,510
            주유량: 52.98L
            합계금액: 80,000
        """.trimIndent()
        val parsed = ReceiptParser.parse(raw)
        assertTrue(parsed.gasStationName?.contains("알뜰") == true)
        assertEquals(52.98, parsed.fuelAmount ?: 0.0, 0.01)
        assertEquals(1510, parsed.unitPrice)
        assertEquals(80000, parsed.totalPrice)
    }

    @Test
    fun testGenericLpgStation() {
        val raw = """
            상호: SK행복충전 동서울충전소
            단가: 1,020원
            수량: 39.22L
            결제금액: 40,000원
        """.trimIndent()
        val parsed = ReceiptParser.parse(raw)
        assertTrue(parsed.gasStationName?.contains("충전소") == true)
        assertEquals(39.22, parsed.fuelAmount ?: 0.0, 0.01)
        assertEquals(1020, parsed.unitPrice)
        assertEquals(40000, parsed.totalPrice)
    }
}
