import sys
import requests

BASE_URL = "http://127.0.0.1:8080"
LOGIN_URL = f"{BASE_URL}/api/v1/auth/login"
VERIFY_OTP_URL = f"{BASE_URL}/api/v1/auth/verify-otp"
AUTH_BEARER = "Bearer {token}"

# Credentials
CREDENTIALS_OTP = {
    "student": ("jjcetpm@jjcet.ac.in", "1234"),
}

CREDENTIALS_LOGIN = {
    "superadmin": ("superadmin_test", "password"),
    "admin": ("admin", "12345"),
    "hod": ("hod", "12345"),
    "faculty": ("faculty", "12345"),
}

AY = "1"
AY_ACADEMIC = "1"

ENDPOINTS = [
    # AnalyticsController (/api/v1/analytics) — uses yearNo param
    ("AnalyticsController", "/api/v1/analytics/institution-growth", {"yearNo": AY}, "list", None, None),
    ("AnalyticsController", "/api/v1/analytics/department-growth", {"yearNo": AY}, "list", None, None),
    ("AnalyticsController", "/api/v1/analytics/xp-curve", {"yearNo": AY}, "list", None, None),
    ("AnalyticsController", "/api/v1/analytics/stage-distribution", {"yearNo": AY}, "list", None, None),
    ("AnalyticsController", "/api/v1/analytics/attendance", {"yearNo": AY}, "list", None, None),
    ("AnalyticsController", "/api/v1/analytics/attendance/calendar", {"yearNo": AY}, "list", None, None),
    ("AnalyticsController", "/api/v1/analytics/activity-funnel", {"yearNo": AY}, "list", None, None),
    ("AnalyticsController", "/api/v1/analytics/performance-leaders",
     {"yearNo": AY, "limit": "10"}, "list", None, None),
    ("AnalyticsController", "/api/v1/analytics/most-improved",
     {"yearNo": AY, "limit": "10"}, "list", None, None),
    ("AnalyticsController", "/api/v1/analytics/intervention", {}, "dict", None, None),  # not implemented

    # AnalyticsDashboardController (/api/v1/analytics) — uses academicYear param
    ("AnalyticsDashboardController", "/api/v1/analytics/dashboard/summary",
     {"academicYear": AY_ACADEMIC}, "dict",
     ["institutionGrowth", "departmentGrowth", "xpCurve", "stageDistribution",
      "attendance", "activityFunnel", "interventionRisk", "topPerformers", "mostImproved"], None),
    ("AnalyticsDashboardController", "/api/v1/analytics/attendance/summary",
     {"academicYear": AY_ACADEMIC}, "dict",
     ["totalStudents", "presentCount", "presentPercentage"], None),
    ("AnalyticsDashboardController", "/api/v1/analytics/xp/distribution",
     {"academicYear": AY_ACADEMIC}, "list", None, None),
    ("AnalyticsDashboardController", "/api/v1/analytics/xp/department/monthly",
     {"academicYear": AY_ACADEMIC}, "list", None, None),
    ("AnalyticsDashboardController", "/api/v1/analytics/xp/department/alltime",
     {"academicYear": AY_ACADEMIC}, "list", None, None),
    ("AnalyticsDashboardController", "/api/v1/analytics/attendance/department/monthly",
     {"academicYear": AY_ACADEMIC}, "list", None, None),
    ("AnalyticsDashboardController", "/api/v1/analytics/leaderboard/top", {}, "list", None, None),
    ("AnalyticsDashboardController", "/api/v1/analytics/teams/elite", {}, "list", None, None),

    # XpAnalyticsController (/api/v1/analytics/xp)
    ("XpAnalyticsController", "/api/v1/analytics/xp/award-penalty", {}, "list", None, None),
    ("XpAnalyticsController", "/api/v1/analytics/xp/departments", {}, "list", None, None),
    ("XpAnalyticsController", "/api/v1/analytics/xp/sections", {}, "list", None, None),
    ("XpAnalyticsController", "/api/v1/analytics/xp/heatmap", {}, "list", None, None),
    ("XpAnalyticsController", "/api/v1/analytics/xp/top-performers", {}, "list", None, None),
    ("XpAnalyticsController", "/api/v1/analytics/xp/low-xp", {}, "list", None, None),
    ("XpAnalyticsController", "/api/v1/analytics/xp/activities", {}, "list", None, None),
    ("XpAnalyticsController", "/api/v1/analytics/xp/history", {}, "dict",
     ["content", "totalPages", "totalElements"], None),
    ("XpAnalyticsController", "/api/v1/analytics/xp/export-history", {}, "binary", None, None),

    # AttendanceAnalyticsController (/api/v1/analytics/attendance)
    ("AttendanceAnalyticsController", "/api/v1/analytics/attendance/overview", {}, "dict",
     ["totalStudents", "presentStudents", "overallAttendancePercentage"], None),
    ("AttendanceAnalyticsController", "/api/v1/analytics/attendance/trend", {}, "list", None, None),
    ("AttendanceAnalyticsController", "/api/v1/analytics/attendance/distribution", {}, "dict",
     ["presentPercentage", "partialAbsentPercentage", "fullAbsentPercentage"], None),
    ("AttendanceAnalyticsController", "/api/v1/analytics/attendance/departments", {}, "list", None, None),
    ("AttendanceAnalyticsController", "/api/v1/analytics/attendance/low-attendance", {}, "list", None, None),
    ("AttendanceAnalyticsController", "/api/v1/analytics/attendance/sections", {}, "list", None, None),
    ("AttendanceAnalyticsController", "/api/v1/analytics/attendance/summary-table", {}, "list", None, None),
    ("AttendanceAnalyticsController", "/api/v1/analytics/attendance/export", {}, "binary", None, None),

    # ScopedAnalyticsControllers — role-scoped dashboard endpoints (uses yearNo param, same envelope)
    ("FacultyScopedAnalyticsController", "/api/v1/faculty/analytics/scoped/dashboard",
     {"yearNo": AY}, "dict",
     ["institutionGrowth", "xpCurve", "stageDistribution", "attendance",
      "activityFunnel", "interventionRisk", "topPerformers", "mostImproved"], "faculty"),
    ("HodScopedAnalyticsController", "/api/v1/hod/analytics/scoped/dashboard",
     {"yearNo": AY}, "dict",
     ["institutionGrowth", "xpCurve", "stageDistribution", "attendance",
      "activityFunnel", "interventionRisk", "topPerformers", "mostImproved"], "hod"),
]

KNOWN_NOT_IMPLEMENTED = {"/api/v1/analytics/intervention"}

BINARY_ENDPOINTS = {
    "/api/v1/analytics/attendance/export",
    "/api/v1/analytics/xp/export-history",
}

EXPECTED_ENVELOPE_KEYS = {"success", "message", "error", "data"}


def login(role: str | None = None) -> str:
    key = role or "admin"
    # First try verify-otp
    if key in CREDENTIALS_OTP:
        email, otp = CREDENTIALS_OTP[key]
        try:
            resp = requests.post(
                VERIFY_OTP_URL,
                json={"email": email, "otp": otp},
                timeout=10,
            )
            if resp.status_code == 200:
                data = resp.json()
                token = data.get("data", {}).get("token")
                if token:
                    print(f"Logged in successfully via OTP as {email} ({key})")
                    return token
        except Exception as e:
            print(f"OTP login exception for {email}: {e}")



    # Fallback to password login
    if key in CREDENTIALS_LOGIN:
        username, password = CREDENTIALS_LOGIN[key]
        resp = requests.post(
            LOGIN_URL,
            json={"username": username, "password": password},
            timeout=10,
        )
        if resp.status_code == 200:
            data = resp.json()
            token = data.get("data", {}).get("token")
            if token:
                print(f"Logged in successfully via password as {username}")
                return token
        raise SystemExit(f"Login failed for {username}: HTTP {resp.status_code} {resp.text}")

    raise SystemExit(f"No login method succeeded for role {role}")


def _is_number(v) -> bool:
    return isinstance(v, (int, float)) and not isinstance(v, bool)


def _validate_percentage(value, label, problems) -> None:
    if value is None:
        return
    if not _is_number(value):
        problems.append(f"{label}: expected numeric percentage, got {type(value).__name__}")
        return
    if not (0.0 <= float(value) <= 100.0):
        problems.append(f"{label}: percentage out of range 0..100 -> {value}")


def _validate_payload(path, expected_type, expected_keys, payload, problems) -> None:
    if expected_type == "dict" and not isinstance(payload, dict):
        problems.append(f"expected data dict, got {type(payload).__name__}")
        return
    if expected_type == "list" and not isinstance(payload, list):
        problems.append(f"expected data list, got {type(payload).__name__}")
        return

    if isinstance(payload, dict) and expected_keys:
        missing = [k for k in expected_keys if k not in payload]
        if missing:
            problems.append(f"data missing keys: {missing}")

    if path.endswith("/attendance/summary") or path.endswith("/attendance/overview"):
        for fld in ("totalStudents", "presentCount"):
            if fld in payload and not _is_number(payload[fld]):
                problems.append(f"data.{fld} not numeric")
        if "presentPercentage" in payload:
            _validate_percentage(payload["presentPercentage"], "data.presentPercentage", problems)

    if path.endswith("/dashboard/summary") or "/scoped/dashboard" in path:
        ig = payload.get("institutionGrowth")
        if not isinstance(ig, list):
            problems.append("dashboard.summary.institutionGrowth should be a list")
        elif ig and (not isinstance(ig[0], dict) or "period" not in ig[0]):
            problems.append("institutionGrowth[0] missing 'period'")

        for key in ("departmentGrowth", "xpCurve", "attendance", "topPerformers", "mostImproved"):
            val = payload.get(key)
            if val is not None and not isinstance(val, list):
                problems.append(f"dashboard.summary.{key} should be a list or null")

        af = payload.get("activityFunnel")
        if af is not None and not isinstance(af, dict):
            problems.append("dashboard.summary.activityFunnel should be a dict (has 'stages')")
        elif isinstance(af, dict):
            if not isinstance(af.get("stages"), list):
                problems.append("activityFunnel.stages should be a list")

        ir = payload.get("interventionRisk")
        if ir is not None and not isinstance(ir, dict):
            problems.append("dashboard.summary.interventionRisk should be a dict")
        elif isinstance(ir, dict):
            for k in ("total", "high", "medium", "low"):
                if k in ir and not _is_number(ir[k]):
                    problems.append(f"interventionRisk.{k} not numeric")
            if "risks" in ir and not isinstance(ir["risks"], list):
                problems.append("interventionRisk.risks should be a list")

    if path.endswith("/performance-leaders") or path.endswith("/most-improved"):
        if isinstance(payload, list) and payload:
            lead = payload[0]
            if not isinstance(lead, dict):
                problems.append("leaderboard row is not an object")
            else:
                if "studentId" not in lead:
                    problems.append("leaderboard row missing studentId")
                if "xp" in lead and not _is_number(lead["xp"]):
                    problems.append("leaderboard row.xp not numeric")
                if "attendance" in lead:
                    _validate_percentage(lead["attendance"], "leaderboard row.attendance", problems)

    if path.endswith("/stage-distribution") and isinstance(payload, list):
        for row in payload:
            if isinstance(row, dict) and "percentage" in row:
                _validate_percentage(row["percentage"], "stage.percentage", problems)

    if path.endswith("/institution-growth") and isinstance(payload, list):
        for row in payload:
            if isinstance(row, dict):
                if "period" not in row:
                    problems.append("institutionGrowth row missing 'period'")

    if path.endswith("/department-growth") and isinstance(payload, list):
        for row in payload:
            if isinstance(row, dict):
                if "departmentId" not in row:
                    problems.append("departmentGrowth row missing 'departmentId'")
                if "departmentName" not in row:
                    problems.append("departmentGrowth row missing 'departmentName'")
                if "growthRate" in row and not _is_number(row["growthRate"]):
                    problems.append("departmentGrowth.growthRate not numeric")

    if path.endswith("/xp-curve") and isinstance(payload, list):
        for row in payload:
            if isinstance(row, dict):
                if "period" not in row:
                    problems.append("xpCurve row missing 'period'")
                if "xp" in row and not _is_number(row["xp"]):
                    problems.append("xpCurve.xp not numeric")

    if path.endswith("/attendance") and isinstance(payload, list):
        for row in payload:
            if isinstance(row, dict):
                if "date" not in row:
                    problems.append("attendance row missing 'date'")
                if "rate" in row:
                    _validate_percentage(row["rate"], "attendance.rate", problems)

    if path.endswith("/activity-funnel") and isinstance(payload, list):
        for row in payload:
            if isinstance(row, dict) and "percentage" in row:
                _validate_percentage(row["percentage"], "funnel.percentage", problems)

    if path.endswith("/attendance/distribution") and isinstance(payload, dict):
        for fld in ("presentPercentage", "partialAbsentPercentage", "fullAbsentPercentage"):
            if fld in payload:
                _validate_percentage(payload[fld], f"distribution.{fld}", problems)

    if path.endswith("/xp/history") and isinstance(payload, dict):
        for fld in ("content", "totalPages", "totalElements"):
            if fld in payload and not isinstance(payload[fld], (list, int)):
                problems.append(f"history.{fld} has unexpected type {type(payload[fld]).__name__}")


def main() -> int:
    admin_token = login("admin")
    faculty_token = login("faculty")
    hod_token = login("hod")

    tokens = {
        "admin": admin_token,
        "faculty": faculty_token,
        "hod": hod_token,
        None: admin_token,
    }

    passed = 0
    failed = 0
    skipped = 0
    rows = []

    for controller, path, params, expected_type, expected_keys, required_role in ENDPOINTS:
        token = tokens.get(required_role, admin_token)
        auth_header = {"Authorization": AUTH_BEARER.format(token=token)}
        try:
            resp = requests.get(
                f"{BASE_URL}{path}",
                params=params,
                headers=auth_header,
                timeout=60,
            )
            code = resp.status_code
        except requests.RequestException as exc:
            rows.append((controller, path, -1, "FAIL", f"request error: {exc}"))
            failed += 1
            continue

        if path in KNOWN_NOT_IMPLEMENTED:
            rows.append((controller, path, code, "SKIP (not implemented)", ""))
            skipped += 1
            continue

        problems = []
        if code < 200 or code >= 300:
            problems.append(f"HTTP {code}: {resp.text[:200]}")
        elif path in BINARY_ENDPOINTS:
            ctype = resp.headers.get("Content-Type", "")
            is_spreadsheet = (
                "spreadsheetml" in ctype
                or "excel" in ctype.lower()
                or "csv" in ctype.lower()
            )
            if not is_spreadsheet:
                problems.append(f"expected spreadsheet content-type, got {ctype}")
        else:
            try:
                body = resp.json()
            except ValueError:
                problems.append("response is not valid JSON")
                body = None

            if body is not None:
                if (isinstance(body, dict)
                        and EXPECTED_ENVELOPE_KEYS.issubset(body.keys())):
                    if body.get("success") is not True:
                        problems.append(f"envelope success != true (got {body.get('success')})")
                    payload = body.get("data")
                else:
                    payload = body

                if payload is None:
                    problems.append("data payload is null")
                elif not isinstance(payload, (dict, list)):
                    problems.append(f"unexpected payload type {type(payload).__name__}")
                else:
                    _validate_payload(path, expected_type, expected_keys, payload, problems)

        if problems:
            status = "FAIL"
            failed += 1
            detail = "; ".join(problems)
        else:
            status = "PASS"
            passed += 1
            detail = ""

        q = ("?" + "&".join(f"{k}={v}" for k, v in params.items())) if params else ""
        rows.append((controller, path + q, code, status, detail))

    r = requests.get(f"{BASE_URL}/api/v1/analytics/dashboard/summary", timeout=30)
    ok = r.status_code in (401, 403)
    try:
        b = r.json()
        env_ok = isinstance(b, dict) and b.get("success") is False
    except ValueError:
        b, env_ok = None, False
    auth_ok = ok and env_ok
    if not auth_ok:
        failed += 1
        auth_detail = f"code={r.status_code} envelope_ok={env_ok}"
    else:
        auth_detail = f"code={r.status_code} envelope_ok={env_ok}"

    print("=" * 100)
    print(f"Analytics endpoint test (+ data validation)  |  base={BASE_URL}  |  academicYear={AY}")
    print("=" * 100)
    print(f"{'CONTROLLER':<28} {'ENDPOINT':<54} {'CODE':>5}  {'STATUS':<22}")
    print("-" * 100)
    for controller, ep, code, status, detail in rows:
        print(f"{controller:<28} {ep:<54} {code:>5}  {status:<22}")
        if detail:
            print(f"{'':<28} {'':<54} {'':>5}  -> {detail}")

    print("-" * 100)
    print(f"ERROR-PATH: [{'PASS' if auth_ok else 'FAIL'}] unauthenticated -> 401/403 + error envelope ({auth_detail})")
    print("-" * 100)
    print(f"PASS={passed}  FAIL={failed}  SKIP(not implemented)={skipped}  TOTAL={len(rows)}")
    print("=" * 100)

    if failed > 0:
        print("RESULT: FAIL - one or more endpoints failed status or data validation.")
        return 1
    print("RESULT: OK - all implemented analytics endpoints passed status + data validation.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
