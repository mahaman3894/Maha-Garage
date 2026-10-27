import subprocess
import os
import sys

fixed_root = os.path.abspath(".")
main_classes = os.path.join(fixed_root, r"app\build\tmp\kotlin-classes\debug")
test_classes = os.path.join(fixed_root, r"app\build\tmp\kotlin-classes\debugUnitTest")

argfile = os.path.join(fixed_root, "java_test_args.txt")
with open(argfile, "r", encoding="cp949", errors="replace") as f:
    old_lines = [line.strip() for line in f if line.strip()]

old_cp = old_lines[1].strip('"')
old_entries = old_cp.split(";")

new_entries = [test_classes, main_classes]
for e in old_entries:
    if "tmp\\kotlin-classes" not in e and "tmp/kotlin-classes" not in e:
        new_entries.append(e)

cp = ";".join(new_entries)
java_exe = r"C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
tests = [
    "maha.garage.PracticalMaintenanceFixTest",
    "maha.garage.QualityGateTest",
    "maha.garage.RegressionTest",
    "maha.garage.MaintenanceScheduleEngineTest",
    "maha.garage.ApplicabilityResolverTest",
    "maha.garage.TireSwapCalculatorTest",
    "maha.garage.TireLifecycleTest",
    "maha.garage.SketchLayoutTest",
    "maha.garage.RenewalDatePolicyTest",
    "maha.garage.RequestedFixesTest",
    "maha.garage.UpdateGatePolicyTest",
    "maha.garage.InsuranceNotificationPolicyTest",
    "maha.garage.AdditionalFixes105Test",
    "maha.garage.FuelCalculatorTest",
    "maha.garage.ReceiptParserTest",
    "maha.garage.OdometerParserTest",
    "maha.garage.VehiclePinningTest",
    "maha.garage.GarageLayoutPolicyTest",
    "maha.garage.TutorialCoordinatesTest",
]

cp_forward = cp.replace("\\", "/")
with open(argfile, "w", encoding="cp949", errors="replace") as f:
    f.write("-cp\n")
    f.write(f'"{cp_forward}"\n')
    f.write("org.junit.runner.JUnitCore\n")
    for t in tests:
        f.write(f"{t}\n")

res = subprocess.run([java_exe, f"@{argfile}"], capture_output=True, text=True, encoding="utf-8", errors="replace")
print("=== STDOUT ===")
print(res.stdout)
print("=== STDERR ===")
print(res.stderr)
print(f"EXIT CODE: {res.returncode}")
sys.exit(res.returncode)
