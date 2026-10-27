package maha.garage

import org.junit.runner.JUnitCore
import org.junit.runner.Result
import org.junit.runner.notification.Failure

fun main() {
    println("==================================================")
    println("   마하개러지 1.1.0 전체 155개 테스트 종합 검증 러너   ")
    println("==================================================")

    val testClasses = arrayOf(
        ApplicabilityResolverTest::class.java,
        MaintenanceScheduleEngineTest::class.java,
        TireSwapCalculatorTest::class.java,
        QualityGateTest::class.java,
        RegressionTest::class.java,
        FuelCalculatorTest::class.java,
        AdditionalFixes105Test::class.java,
        RenewalDatePolicyTest::class.java,
        RequestedFixesTest::class.java,
        TireLifecycleTest::class.java,
        UpdateGatePolicyTest::class.java,
        VehiclePinningTest::class.java,
        InsuranceNotificationPolicyTest::class.java,
        OdometerParserTest::class.java,
        PracticalMaintenanceFixTest::class.java,
        ReceiptParserTest::class.java,
        SketchLayoutTest::class.java
    )

    var totalTests = 0
    var totalFailures = 0
    val allFailures = mutableListOf<Failure>()

    for (testClass in testClasses) {
        val result: Result = JUnitCore.runClasses(testClass)
        totalTests += result.runCount
        totalFailures += result.failureCount
        allFailures.addAll(result.failures)

        val status = if (result.wasSuccessful()) "[PASS]" else "[FAIL]"
        println("  $status ${testClass.simpleName.padEnd(35)}: ${result.runCount - result.failureCount}/${result.runCount} 성공")
    }

    println("\n==================================================")
    println("  검증 결과 종합: 총 $totalTests 개 테스트 중 ${totalTests - totalFailures} 개 성공, $totalFailures 개 실패")
    println("==================================================")

    if (totalFailures > 0) {
        println("\n실패 상세 내역:")
        for (f in allFailures) {
            println("  - [실패] ${f.testHeader}")
            println("    메시지: ${f.message}")
            f.exception?.printStackTrace()
        }
        System.exit(1)
    } else {
        println("\n🎉 축하합니다! 모든 테스트 ($totalTests/$totalTests)가 100% 통과되었습니다!")
    }
}
