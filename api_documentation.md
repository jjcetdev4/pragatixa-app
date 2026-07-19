# Complete API Documentation

## XpController
--------------------------------------------------
**Controller Name:** XpController
**Package:** com.spdms.student
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/xp/{studentId}/summary
**Java Method:** getXpSummary
**Purpose:** Returns total XP points earned by category.
**Request Type:**
- Path Variables: studentId
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<Map<String, Integer>>
**Authentication Required:** Yes
**Authorization:** None specified
**Service Called:** xpService.getXpSummary
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 401 Unauthorized, 403 Forbidden
--------------------------------------------------
**Controller Name:** XpController
**Package:** com.spdms.student
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/xp/{studentId}/history
**Java Method:** getXpHistory
**Purpose:** Returns a paginated list of XP transactions for a student.
**Request Type:**
- Path Variables: studentId
- Query Parameters: page, size
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<Page<XpTransaction>>
**Authentication Required:** Yes
**Authorization:** None specified
**Service Called:** xpService.getXpHistory
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 401 Unauthorized
--------------------------------------------------
**Controller Name:** XpController
**Package:** com.spdms.student
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/xp/{studentId}/streaks
**Java Method:** getStudentStreaks
**Purpose:** Returns all coding, diary, and library streaks for a student.
**Request Type:**
- Path Variables: studentId
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<List<Streak>>
**Authentication Required:** Yes
**Authorization:** None specified
**Service Called:** xpService.getStudentStreaks
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** XpController
**Package:** com.spdms.student
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/xp/submit
**Java Method:** submitXpClaim
**Purpose:** Allows a student to submit evidence link for an activity.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: ClaimSubmissionRequest
**Request DTO:** ClaimSubmissionRequest
**Response DTO:** ApiResponse<XpTransaction>
**Authentication Required:** Yes
**Authorization:** Student Role
**Service Called:** xpService.submitXpClaim
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 400 Bad Request
--------------------------------------------------
**Controller Name:** XpController
**Package:** com.spdms.student
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/xp/{id}/approve
**Java Method:** approveXpClaim
**Purpose:** Approves a pending student XP claim. Requires Faculty or Admin role.
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<XpTransaction>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** xpService.approveXpClaim
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 400 Bad Request, 403 Forbidden
--------------------------------------------------
**Controller Name:** XpController
**Package:** com.spdms.student
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/xp/{id}/reject
**Java Method:** rejectXpClaim
**Purpose:** Rejects a pending student XP claim. Requires Faculty or Admin role.
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<XpTransaction>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** xpService.rejectXpClaim
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 400 Bad Request, 403 Forbidden
--------------------------------------------------
**Controller Name:** XpController
**Package:** com.spdms.student
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/xp/penalty
**Java Method:** logViolation
**Purpose:** Deducts XP points from a student for a discipline infraction. Requires Faculty or Admin role.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: LogViolationRequest
**Request DTO:** LogViolationRequest
**Response DTO:** ApiResponse<XpTransaction>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** xpService.logViolation
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 400 Bad Request

## LevelBadgeController
--------------------------------------------------
**Controller Name:** LevelBadgeController
**Package:** com.spdms.student
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/levels
**Java Method:** getAllLevels
**Purpose:** Returns the JJCET 8-level individual progression stepper limits.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<List<Level>>
**Authentication Required:** Yes
**Authorization:** None specified
**Service Called:** levelBadgeService.getAllLevels
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** LevelBadgeController
**Package:** com.spdms.student
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/levels/student/{studentId}/current
**Java Method:** getCurrentLevelForStudent
**Purpose:** Determines a student's active level based on their current XP score.
**Request Type:**
- Path Variables: studentId
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<Level>
**Authentication Required:** Yes
**Authorization:** None specified
**Service Called:** levelBadgeService.getCurrentLevelForStudent
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 404 Not Found
--------------------------------------------------
**Controller Name:** LevelBadgeController
**Package:** com.spdms.student
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/levels/me/current
**Java Method:** getCurrentLoggedInLevel
**Purpose:** Get Current Logged-in Student's Level
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<Level>
**Authentication Required:** Yes
**Authorization:** None specified
**Service Called:** levelBadgeService.getCurrentLevelForStudent
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 404 Not Found
--------------------------------------------------
**Controller Name:** LevelBadgeController
**Package:** com.spdms.student
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/badges
**Java Method:** getAllBadges
**Purpose:** Returns all badges in all 5 tiers.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<List<Badge>>
**Authentication Required:** Yes
**Authorization:** None specified
**Service Called:** levelBadgeService.getAllBadges
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** LevelBadgeController
**Package:** com.spdms.student
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/badges/student/me
**Java Method:** getMyBadges
**Purpose:** Returns all earned and pending badge claims for the active student session.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<List<StudentBadgeResponse>>
**Authentication Required:** Yes
**Authorization:** None specified
**Service Called:** levelBadgeService.getBadgesForStudent
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** LevelBadgeController
**Package:** com.spdms.student
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/badges/student/{studentId}
**Java Method:** getBadgesForStudent
**Purpose:** Get Badges by Student ID
**Request Type:**
- Path Variables: studentId
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<List<StudentBadgeResponse>>
**Authentication Required:** Yes
**Authorization:** None specified
**Service Called:** levelBadgeService.getBadgesForStudent
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** LevelBadgeController
**Package:** com.spdms.student
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/badges/submit
**Java Method:** submitBadgeClaim
**Purpose:** Student submits a badge claim with evidence URL.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: ClaimBadgeRequest
**Request DTO:** ClaimBadgeRequest
**Response DTO:** ApiResponse<StudentBadgeResponse>
**Authentication Required:** Yes
**Authorization:** None specified
**Service Called:** levelBadgeService.submitBadgeClaim
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 400 Bad Request
--------------------------------------------------
**Controller Name:** LevelBadgeController
**Package:** com.spdms.student
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/badges/{id}/approve
**Java Method:** approveBadgeClaim
**Purpose:** Approves a pending badge claim. Requires Faculty or Admin role.
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<StudentBadgeResponse>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** levelBadgeService.approveBadgeClaim
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 400 Bad Request, 403 Forbidden
--------------------------------------------------
**Controller Name:** LevelBadgeController
**Package:** com.spdms.student
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/badges/{id}/reject
**Java Method:** rejectBadgeClaim
**Purpose:** Rejects a pending badge claim. Requires Faculty or Admin role.
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<StudentBadgeResponse>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** levelBadgeService.rejectBadgeClaim
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 400 Bad Request, 403 Forbidden
--------------------------------------------------
**Controller Name:** LevelBadgeController
**Package:** com.spdms.student
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/badges/pending
**Java Method:** getPendingBadgeClaims
**Purpose:** Returns all student badge claims with PENDING status. Requires Faculty or Admin role.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ApiResponse<List<StudentBadgeResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** levelBadgeService.getPendingBadgeClaims
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 403 Forbidden

## StudentXpController
--------------------------------------------------
**Controller Name:** StudentXpController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/my-activities/{activityId}/years
**Java Method:** getYearsForActivity
**Purpose:** Get distinct years assigned to the activity that the teacher has permission to view
**Request Type:**
- Path Variables: activityId
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<String>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** queryService.getYearsForActivity
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 403 Forbidden
--------------------------------------------------
**Controller Name:** StudentXpController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/my-activities/{activityId}/departments
**Java Method:** getDepartmentsForActivity
**Purpose:** Get distinct departments assigned to the activity/year that the teacher has permission to view
**Request Type:**
- Path Variables: activityId
- Query Parameters: year
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Map<String, Object>>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** queryService.getDepartmentsForActivity
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 403 Forbidden
--------------------------------------------------
**Controller Name:** StudentXpController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/my-activities/{activityId}/sections
**Java Method:** getSectionsForActivity
**Purpose:** Get distinct sections assigned to the activity/year/department that the teacher has permission to view
**Request Type:**
- Path Variables: activityId
- Query Parameters: year, departmentId
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Map<String, Object>>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** queryService.getSectionsForActivity
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 403 Forbidden
--------------------------------------------------
**Controller Name:** StudentXpController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/my-activities/{activityId}/students
**Java Method:** getStudentsForActivity
**Purpose:** Get list of students eligible for the given assigned activity
**Request Type:**
- Path Variables: activityId
- Query Parameters: year, departmentId, sectionId
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<MyActivityStudentsResponse>>
**Authentication Required:** Yes
**Authorization:** TEACHER
**Service Called:** queryService.getStudentsForActivity
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 403 Forbidden
--------------------------------------------------
**Controller Name:** StudentXpController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/student-xp/award
**Java Method:** awardStudentXp
**Purpose:** Award XP points to a student for a specific activity
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: AwardXpRequest
**Request DTO:** AwardXpRequest
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** TEACHER
**Service Called:** studentXpService.awardStudentXp
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 403 Forbidden
--------------------------------------------------
**Controller Name:** StudentXpController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/student-xp/award/batch
**Java Method:** awardStudentXpBatch
**Purpose:** Award XP points to multiple students for a specific activity
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: AwardXpRequest
**Request DTO:** AwardXpRequest
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** TEACHER
**Service Called:** studentXpService.awardStudentXpBatch
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 403 Forbidden

## StudentController
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/students
**Java Method:** createStudent
**Purpose:** Creates a new student record. Requires ADMIN or TEACHER role.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: CreateStudentRequest
**Request DTO:** CreateStudentRequest
**Response DTO:** ResponseEntity<ApiResponse<StudentResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** studentService.createStudent
**Repository Called:** Varies by service
**Response Codes:** 201 Created, 400 Bad Request, 403 Forbidden
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/students
**Java Method:** getAllStudents
**Purpose:** Returns paginated list of all students.
**Request Type:**
- Path Variables: None
- Query Parameters: page, size, sortBy, year, section
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Page<StudentResponse>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER, STUDENT
**Service Called:** studentService.getAllStudents
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/students/{id}
**Java Method:** getStudentById
**Purpose:** Get Student by ID
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<StudentResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** studentService.getStudentById
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 404 Not Found
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/students/search
**Java Method:** searchStudents
**Purpose:** Search by name, student ID, or email.
**Request Type:**
- Path Variables: None
- Query Parameters: keyword, page, size
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Page<StudentResponse>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** studentService.searchStudents
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** DELETE
**Complete Endpoint URL:** /api/v1/students/{id}
**Java Method:** deleteStudent
**Purpose:** Deletes a student record. Requires ADMIN role.
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** studentService.deleteStudent
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 404 Not Found, 403 Forbidden
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/students/{id}
**Java Method:** updateStudent
**Purpose:** Updates student profile details. Requires ADMIN or TEACHER role.
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: UpdateStudentRequest
**Request DTO:** UpdateStudentRequest
**Response DTO:** ResponseEntity<ApiResponse<StudentResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** studentService.updateStudent
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 404 Not Found
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/students/bulk-parse
**Java Method:** bulkParseStudents
**Purpose:** Parses Excel and returns JSON preview list of student records without saving. Requires ADMIN or TEACHER role.
**Request Type:**
- Path Variables: None
- Query Parameters: file
- Request Body: None
**Request DTO:** MultipartFile
**Response DTO:** ResponseEntity<ApiResponse<List<CreateStudentRequest>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** studentService.bulkParse
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 400 Bad Request
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/students/bulk-import
**Java Method:** bulkImportStudents
**Purpose:** Saves selected list of parsed student records into the database. Requires ADMIN or TEACHER role.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: List<CreateStudentRequest>
**Request DTO:** List<CreateStudentRequest>
**Response DTO:** ResponseEntity<ApiResponse<String>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** studentService.bulkImport
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 400 Bad Request
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/students/{id}/adjust-points
**Java Method:** adjustPoints
**Purpose:** Adds or deducts points for a student. Checks activity-faculty assignments.
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: PointAdjustmentRequest
**Request DTO:** PointAdjustmentRequest
**Response DTO:** ResponseEntity<ApiResponse<StudentResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** studentService.adjustPoints
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 403 Forbidden
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/students/{id}/discipline-logs
**Java Method:** getDisciplineLogs
**Purpose:** Fetch history logs of points adjustments for a student.
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<DisciplineLog>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER, STUDENT
**Service Called:** studentService.getDisciplineLogs
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/students/department-performance
**Java Method:** getDepartmentPerformance
**Purpose:** Returns overall and year-wise average discipline scores. Requires sub-role HOD.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<DepartmentPerformanceResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** studentService.getDepartmentPerformance
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 403 Forbidden
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/students/{id}/make-captain
**Java Method:** promoteToTeamCaptain
**Purpose:** Promotes Student to Team Captain
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** studentService.promoteToTeamCaptain
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 400 Bad Request
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/students/{id}/remove-captain
**Java Method:** removeTeamCaptain
**Purpose:** Removes Student from Team Captain status
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** studentService.removeTeamCaptain
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 400 Bad Request
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/students/stages
**Java Method:** getStudentStages
**Purpose:** Get Stages Configured for Student
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<?>
**Authentication Required:** Yes
**Authorization:** STUDENT
**Service Called:** studentStageFacade.getStudentStages
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** StudentController
**Package:** com.spdms.modules.student.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/students/subgroups/{subgroupId}/activities
**Java Method:** getActivitiesBySubgroup
**Purpose:** Get all activities of a subgroup
**Request Type:**
- Path Variables: subgroupId
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Activity>>>
**Authentication Required:** Yes
**Authorization:** STUDENT, ADMIN, TEACHER
**Service Called:** activityRepository.findBySubgroupId
**Repository Called:** activityRepository
**Response Codes:** 200 OK

## GroupActivityController
--------------------------------------------------
**Controller Name:** GroupActivityController
**Package:** com.spdms.modules.activity.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/group-activities/assignments/{assignmentId}/teams
**Java Method:** getTeamsForAssignment
**Purpose:** Returns all teams created for a specific activity assignment.
**Request Type:**
- Path Variables: assignmentId
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<TeamResponse>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** teamRepository.findByAssignmentId
**Repository Called:** teamRepository
**Response Codes:** 200 OK, 404 Not Found
--------------------------------------------------
**Controller Name:** GroupActivityController
**Package:** com.spdms.modules.activity.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/group-activities/teams/{teamId}/award-xp
**Java Method:** awardXpToTeam
**Purpose:** Awards XP to all or selected members of a team with remarks.
**Request Type:**
- Path Variables: teamId
- Query Parameters: None
- Request Body: Map<String, Object>
**Request DTO:** Map<String, Object>
**Response DTO:** ResponseEntity<ApiResponse<String>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** teamRepository.findById
**Repository Called:** teamRepository, studentActivityXpRepository
**Response Codes:** 200 OK, 400 Bad Request, 401 Unauthorized, 404 Not Found

## AuthController
--------------------------------------------------
**Controller Name:** AuthController
**Package:** com.spdms.modules.authentication.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/auth/login
**Java Method:** login
**Purpose:** Authenticate username & password. Returns a JWT token.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: LoginRequest
**Request DTO:** LoginRequest
**Response DTO:** ResponseEntity<ApiResponse<AuthResponse>>
**Authentication Required:** No
**Authorization:** None
**Service Called:** authService.loginUser
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 401 Unauthorized
--------------------------------------------------
**Controller Name:** AuthController
**Package:** com.spdms.modules.authentication.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/auth/student-login
**Java Method:** studentLogin
**Purpose:** Authenticate using Student ID (or email) & password. Returns a JWT token.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: StudentLoginRequest
**Request DTO:** StudentLoginRequest
**Response DTO:** ResponseEntity<ApiResponse<AuthResponse>>
**Authentication Required:** No
**Authorization:** None
**Service Called:** authService.loginStudent
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 401 Unauthorized
--------------------------------------------------
**Controller Name:** AuthController
**Package:** com.spdms.modules.authentication.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/auth/me
**Java Method:** getProfile
**Purpose:** Returns profile details of the logged in user based on the JWT token.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<AuthResponse>>
**Authentication Required:** Yes
**Authorization:** None specified (Implicit valid token)
**Service Called:** authService.getUserProfile
**Repository Called:** Varies by service
**Response Codes:** 200 OK, 401 Unauthorized

## AdminDashboardController
--------------------------------------------------
**Controller Name:** AdminDashboardController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/stats
**Java Method:** getDashboardStats
**Purpose:** Get overview metrics for admin dashboard.
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Map<String, Object>>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminDashboardService.getDashboardStats
**Repository Called:** Varies by service
**Response Codes:** 200 OK

## AdminLookupController
--------------------------------------------------
**Controller Name:** AdminLookupController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/academic-years
**Java Method:** getAllAcademicYears
**Purpose:** List Academic Years
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<AcademicYear>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** adminLookupService.getAllAcademicYears
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminLookupController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/years
**Java Method:** getAllYears
**Purpose:** List Years
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Year>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** adminLookupService.getAllYears
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminLookupController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/semesters
**Java Method:** getAllSemesters
**Purpose:** List Semesters
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Semester>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** adminLookupService.getAllSemesters
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminLookupController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/genders
**Java Method:** getAllGenders
**Purpose:** List Genders
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Gender>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** adminLookupService.getAllGenders
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminLookupController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/sections
**Java Method:** getAllSections
**Purpose:** List Sections
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Section>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** adminLookupService.getAllSections
**Repository Called:** Varies by service
**Response Codes:** 200 OK

## AdminFacultyController
--------------------------------------------------
**Controller Name:** AdminFacultyController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/admin/subgroups/{id}/assign-faculty
**Java Method:** assignFacultyToSubgroup
**Purpose:** Assign a faculty member to an activity subgroup
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: Map<String, Object>
**Request DTO:** Map<String, Object>
**Response DTO:** ResponseEntity<ApiResponse<ActivitySubgroup>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminFacultyService.assignFacultyToSubgroup
**Repository Called:** Varies by service
**Response Codes:** 200 OK

## AdminStageController
--------------------------------------------------
**Controller Name:** AdminStageController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/stages
**Java Method:** getAllStages
**Purpose:** Get all activity stages with subgroups
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<ActivityStageResponse>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER, STUDENT
**Service Called:** adminStageService.getAllStages
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminStageController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/admin/stages
**Java Method:** createStage
**Purpose:** Create a new stage
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: ActivityStageRequest
**Request DTO:** ActivityStageRequest
**Response DTO:** ResponseEntity<ApiResponse<ActivityStageResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminStageService.createStage
**Repository Called:** Varies by service
**Response Codes:** 201 Created
--------------------------------------------------
**Controller Name:** AdminStageController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/stages/{id}
**Java Method:** getStage
**Purpose:** Get activity stage by ID
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<ActivityStageResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER, STUDENT
**Service Called:** adminStageService.getStage
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminStageController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/admin/stages/{id}
**Java Method:** editStage
**Purpose:** Update an existing stage
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: ActivityStageRequest
**Request DTO:** ActivityStageRequest
**Response DTO:** ResponseEntity<ApiResponse<ActivityStageResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminStageService.editStage
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminStageController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/stages/{id}/report
**Java Method:** getStageReport
**Purpose:** Get stage completion report
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Map<String, Object>>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminStageService.getStageReport
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminStageController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** DELETE
**Complete Endpoint URL:** /api/v1/admin/stages/{id}
**Java Method:** deleteStage
**Purpose:** Delete a stage and its subgroups
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminStageService.deleteStage
**Repository Called:** Varies by service
**Response Codes:** 200 OK

## AdminRoleController
--------------------------------------------------
**Controller Name:** AdminRoleController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/roles
**Java Method:** getAllRoles
**Purpose:** List Roles
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Role>>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminRoleService.getAllRoles
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminRoleController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/admin/roles
**Java Method:** createRole
**Purpose:** Create Role
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: Map<String, String>
**Request DTO:** Map<String, String>
**Response DTO:** ResponseEntity<ApiResponse<Role>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminRoleService.createRole
**Repository Called:** Varies by service
**Response Codes:** 200 OK

## AdminDepartmentController
--------------------------------------------------
**Controller Name:** AdminDepartmentController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/departments
**Java Method:** getAllDepartments
**Purpose:** List Departments
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Map<String, Object>>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** adminDepartmentService.getAllDepartments
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminDepartmentController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/admin/departments
**Java Method:** createDepartment
**Purpose:** Create Department
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: CreateDepartmentRequest
**Request DTO:** CreateDepartmentRequest
**Response DTO:** ResponseEntity<ApiResponse<Department>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminDepartmentService.createDepartment
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminDepartmentController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/admin/departments/{id}
**Java Method:** updateDepartment
**Purpose:** Update Department
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: CreateDepartmentRequest
**Request DTO:** CreateDepartmentRequest
**Response DTO:** ResponseEntity<ApiResponse<Department>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminDepartmentService.updateDepartment
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminDepartmentController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** DELETE
**Complete Endpoint URL:** /api/v1/admin/departments/{id}
**Java Method:** deleteDepartment
**Purpose:** Delete Department
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminDepartmentService.deleteDepartment
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminDepartmentController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/departments/{id}/sections
**Java Method:** getSectionsOfDept
**Purpose:** Get Sections of Department
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Section>>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminDepartmentService.getSectionsOfDept
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminDepartmentController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/admin/departments/{id}/sections
**Java Method:** createSection
**Purpose:** Create Section for Department
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: Map<String, Object>
**Request DTO:** Map<String, Object>
**Response DTO:** ResponseEntity<ApiResponse<Section>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminDepartmentService.createSection
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminDepartmentController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** DELETE
**Complete Endpoint URL:** /api/v1/admin/departments/{id}/sections/{sectionId}
**Java Method:** deleteSection
**Purpose:** Delete Section from Department
**Request Type:**
- Path Variables: id, sectionId
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminDepartmentService.deleteSection
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminDepartmentController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/departments/class-coordinators
**Java Method:** getClassCoordinators
**Purpose:** Get all class coordinators mapped by department and section
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Map<String, Object>>>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminDepartmentService.getClassCoordinators
**Repository Called:** Varies by service
**Response Codes:** 200 OK

## AdminSubgroupController
--------------------------------------------------
**Controller Name:** AdminSubgroupController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/admin/stages/{stageId}/subgroups
**Java Method:** createSubgroup
**Purpose:** Create a subgroup under a stage
**Request Type:**
- Path Variables: stageId
- Query Parameters: None
- Request Body: Map<String, Object>
**Request DTO:** Map<String, Object>
**Response DTO:** ResponseEntity<ApiResponse<ActivitySubgroup>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminSubgroupService.createSubgroup
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminSubgroupController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/admin/subgroups/{id}
**Java Method:** updateSubgroup
**Purpose:** Update a subgroup's name or threshold
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: Map<String, Object>
**Request DTO:** Map<String, Object>
**Response DTO:** ResponseEntity<ApiResponse<ActivitySubgroup>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminSubgroupService.updateSubgroup
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminSubgroupController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** DELETE
**Complete Endpoint URL:** /api/v1/admin/subgroups/{id}
**Java Method:** deleteSubgroup
**Purpose:** Delete a subgroup
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminSubgroupService.deleteSubgroup
**Repository Called:** Varies by service
**Response Codes:** 200 OK

## AdminSubjectController
--------------------------------------------------
**Controller Name:** AdminSubjectController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/subjects
**Java Method:** getAllSubjects
**Purpose:** Get all dynamic subjects
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Subject>>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminSubjectService.getAllSubjects
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminSubjectController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/admin/subjects
**Java Method:** createSubject
**Purpose:** Create a dynamic subject
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: Map<String, String>
**Request DTO:** Map<String, String>
**Response DTO:** ResponseEntity<ApiResponse<Subject>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminSubjectService.createSubject
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminSubjectController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** DELETE
**Complete Endpoint URL:** /api/v1/admin/subjects/{id}
**Java Method:** deleteSubject
**Purpose:** Delete a dynamic subject
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminSubjectService.deleteSubject
**Repository Called:** Varies by service
**Response Codes:** 200 OK

## AdminActivityController
--------------------------------------------------
**Controller Name:** AdminActivityController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/my-activities
**Java Method:** getMyActivities
**Purpose:** Get activities assigned to the currently logged in teacher
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<MyActivityResponse>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** adminActivityService.getMyActivities
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminActivityController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/subgroups/{subgroupId}/activities
**Java Method:** getActivitiesBySubgroup
**Purpose:** Get all activities of a subgroup
**Request Type:**
- Path Variables: subgroupId
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Activity>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER, STUDENT
**Service Called:** adminActivityService.getActivitiesBySubgroup
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminActivityController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/admin/subgroups/{subgroupId}/activities
**Java Method:** createActivity
**Purpose:** Create a new activity under a subgroup
**Request Type:**
- Path Variables: subgroupId
- Query Parameters: None
- Request Body: Map<String, Object>
**Request DTO:** Map<String, Object>
**Response DTO:** ResponseEntity<ApiResponse<Activity>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminActivityService.createActivity
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminActivityController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/admin/activities/{activityId}
**Java Method:** updateActivity
**Purpose:** Update an activity
**Request Type:**
- Path Variables: activityId
- Query Parameters: None
- Request Body: Map<String, Object>
**Request DTO:** Map<String, Object>
**Response DTO:** ResponseEntity<ApiResponse<Activity>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminActivityService.updateActivity
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminActivityController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/admin/activities/{id}/assign
**Java Method:** assignActivity
**Purpose:** Assign departments/sections/faculty to an activity
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: Map<String, Object>
**Request DTO:** Map<String, Object>
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminActivityService.assignActivity
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminActivityController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** DELETE
**Complete Endpoint URL:** /api/v1/admin/activities/{activityId}
**Java Method:** deleteActivity
**Purpose:** Delete an activity
**Request Type:**
- Path Variables: activityId
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminActivityService.deleteActivity
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminActivityController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/frequencies/custom
**Java Method:** getCustomFrequencies
**Purpose:** Get all custom award frequencies
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<CustomFrequency>>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminActivityService.getCustomFrequencies
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminActivityController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/admin/frequencies/custom
**Java Method:** createCustomFrequency
**Purpose:** Create a custom award frequency
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: Map<String, Object>
**Request DTO:** Map<String, Object>
**Response DTO:** ResponseEntity<ApiResponse<CustomFrequency>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminActivityService.createCustomFrequency
**Repository Called:** Varies by service
**Response Codes:** 200 OK

## AdminUserController
--------------------------------------------------
**Controller Name:** AdminUserController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/admin/users
**Java Method:** getAllUsers
**Purpose:** List All Users
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<UserResponse>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** adminUserService.getAllUsers
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminUserController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/admin/users
**Java Method:** createUser
**Purpose:** Create User
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: CreateUserRequest
**Request DTO:** CreateUserRequest
**Response DTO:** ResponseEntity<ApiResponse<UserResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminUserService.createUser
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminUserController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/admin/users/{id}
**Java Method:** updateUser
**Purpose:** Update User
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: UpdateUserRequest
**Request DTO:** UpdateUserRequest
**Response DTO:** ResponseEntity<ApiResponse<UserResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminUserService.updateUser
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** AdminUserController
**Package:** com.spdms.modules.admin.controller
**HTTP Method:** DELETE
**Complete Endpoint URL:** /api/v1/admin/users/{id}
**Java Method:** deleteUser
**Purpose:** Delete User
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN
**Service Called:** adminUserService.deleteUser
**Repository Called:** Varies by service
**Response Codes:** 200 OK

## TeamController
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/teams
**Java Method:** createTeam
**Purpose:** Create Team
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: CreateTeamRequest
**Request DTO:** CreateTeamRequest
**Response DTO:** ResponseEntity<ApiResponse<TeamResponse>>
**Authentication Required:** Yes
**Authorization:** TEACHER, ADMIN
**Service Called:** teamCrudService.createTeam
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/teams
**Java Method:** getAllTeams
**Purpose:** List Teams
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<TeamResponse>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** teamQueryService.getAllTeams
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/teams/my-team
**Java Method:** getMyTeam
**Purpose:** Get My Team
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<TeamResponse>>
**Authentication Required:** Yes
**Authorization:** STUDENT, TEACHER, ADMIN
**Service Called:** teamQueryService.getMyTeam
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/teams/{id}
**Java Method:** getTeamById
**Purpose:** Get Team by ID
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<TeamResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER, STUDENT
**Service Called:** teamQueryService.getTeamById
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/teams/{id}
**Java Method:** updateTeam
**Purpose:** Update Team
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: CreateTeamRequest
**Request DTO:** CreateTeamRequest
**Response DTO:** ResponseEntity<ApiResponse<TeamResponse>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** teamCrudService.updateTeam
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/teams/{id}/members
**Java Method:** addMemberToTeam
**Purpose:** Add Team Member by Team ID
**Request Type:**
- Path Variables: id
- Query Parameters: studentId
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** teamMemberService.addMemberToTeam
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** DELETE
**Complete Endpoint URL:** /api/v1/teams/{id}/members/{studentId}
**Java Method:** removeMemberFromTeam
**Purpose:** Remove Team Member by Team ID
**Request Type:**
- Path Variables: id, studentId
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** teamMemberService.removeMemberFromTeam
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/teams/{id}/captain
**Java Method:** assignTeamCaptain
**Purpose:** Assign Team Captain
**Request Type:**
- Path Variables: id
- Query Parameters: studentId
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** teamMemberService.assignTeamCaptain
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/teams/my-classmates
**Java Method:** getMyClassmates
**Purpose:** Get My Classmates
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<Map<String, Object>>>>
**Authentication Required:** Yes
**Authorization:** STUDENT
**Service Called:** teamQueryService.getMyClassmates
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/teams/my-team/add-member
**Java Method:** addMember
**Purpose:** Add Team Member
**Request Type:**
- Path Variables: None
- Query Parameters: studentId
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** STUDENT, TEACHER, ADMIN
**Service Called:** teamMemberService.addMemberByStudent
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/teams/{id}/add-member
**Java Method:** addMemberByCC
**Purpose:** Add Team Member (CC)
**Request Type:**
- Path Variables: id
- Query Parameters: studentId
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** TEACHER, ADMIN
**Service Called:** teamMemberService.addMemberByCC
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/teams/{id}/remove-member
**Java Method:** removeMemberByCC
**Purpose:** Remove Team Member (CC)
**Request Type:**
- Path Variables: id
- Query Parameters: studentId
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** TEACHER, ADMIN
**Service Called:** teamMemberService.removeMemberByCC
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** POST
**Complete Endpoint URL:** /api/v1/teams/my-team/remove-request
**Java Method:** requestRemoveMember
**Purpose:** Request Team Member Removal
**Request Type:**
- Path Variables: None
- Query Parameters: studentId, reason
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** STUDENT, TEACHER, ADMIN
**Service Called:** teamRequestService.requestRemoveMember
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** GET
**Complete Endpoint URL:** /api/v1/teams/removal-requests/pending
**Java Method:** getPendingRemovalRequests
**Purpose:** Get Pending Removal Requests
**Request Type:**
- Path Variables: None
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<List<TeamRemovalRequestDto>>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** teamRequestService.getPendingRemovalRequests
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/teams/removal-requests/{id}/approve
**Java Method:** approveRemovalRequest
**Purpose:** Approve Removal Request
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** teamRequestService.approveRemovalRequest
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/teams/removal-requests/{id}/reject
**Java Method:** rejectRemovalRequest
**Purpose:** Reject Removal Request
**Request Type:**
- Path Variables: id
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** ADMIN, TEACHER
**Service Called:** teamRequestService.rejectRemovalRequest
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** PUT
**Complete Endpoint URL:** /api/v1/teams/{id}/limit
**Java Method:** updateTeamLimit
**Purpose:** Update Team Limit
**Request Type:**
- Path Variables: id
- Query Parameters: size
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** TEACHER, ADMIN
**Service Called:** teamCrudService.updateTeamLimit
**Repository Called:** Varies by service
**Response Codes:** 200 OK
--------------------------------------------------
**Controller Name:** TeamController
**Package:** com.spdms.admin
**HTTP Method:** DELETE
**Complete Endpoint URL:** /api/v1/teams/{teamId}
**Java Method:** deleteTeam
**Purpose:** Delete Team
**Request Type:**
- Path Variables: teamId
- Query Parameters: None
- Request Body: None
**Request DTO:** None
**Response DTO:** ResponseEntity<ApiResponse<Void>>
**Authentication Required:** Yes
**Authorization:** TEACHER, ADMIN
**Service Called:** teamCrudService.deleteTeam
**Repository Called:** Varies by service
**Response Codes:** 200 OK

# API Summary Table

| No | Method | Endpoint | Controller |
|---|---|---|---|
| 1 | GET | /api/v1/xp/{studentId}/summary | XpController |
| 2 | GET | /api/v1/xp/{studentId}/history | XpController |
| 3 | GET | /api/v1/xp/{studentId}/streaks | XpController |
| 4 | POST | /api/v1/xp/submit | XpController |
| 5 | PUT | /api/v1/xp/{id}/approve | XpController |
| 6 | PUT | /api/v1/xp/{id}/reject | XpController |
| 7 | POST | /api/v1/xp/penalty | XpController |
| 8 | GET | /api/v1/levels | LevelBadgeController |
| 9 | GET | /api/v1/levels/student/{studentId}/current | LevelBadgeController |
| 10 | GET | /api/v1/levels/me/current | LevelBadgeController |
| 11 | GET | /api/v1/badges | LevelBadgeController |
| 12 | GET | /api/v1/badges/student/me | LevelBadgeController |
| 13 | GET | /api/v1/badges/student/{studentId} | LevelBadgeController |
| 14 | POST | /api/v1/badges/submit | LevelBadgeController |
| 15 | PUT | /api/v1/badges/{id}/approve | LevelBadgeController |
| 16 | PUT | /api/v1/badges/{id}/reject | LevelBadgeController |
| 17 | GET | /api/v1/badges/pending | LevelBadgeController |
| 18 | GET | /api/v1/my-activities/{activityId}/years | StudentXpController |
| 19 | GET | /api/v1/my-activities/{activityId}/departments | StudentXpController |
| 20 | GET | /api/v1/my-activities/{activityId}/sections | StudentXpController |
| 21 | GET | /api/v1/my-activities/{activityId}/students | StudentXpController |
| 22 | POST | /api/v1/student-xp/award | StudentXpController |
| 23 | POST | /api/v1/student-xp/award/batch | StudentXpController |
| 24 | POST | /api/v1/students | StudentController |
| 25 | GET | /api/v1/students | StudentController |
| 26 | GET | /api/v1/students/{id} | StudentController |
| 27 | GET | /api/v1/students/search | StudentController |
| 28 | DELETE | /api/v1/students/{id} | StudentController |
| 29 | PUT | /api/v1/students/{id} | StudentController |
| 30 | POST | /api/v1/students/bulk-parse | StudentController |
| 31 | POST | /api/v1/students/bulk-import | StudentController |
| 32 | POST | /api/v1/students/{id}/adjust-points | StudentController |
| 33 | GET | /api/v1/students/{id}/discipline-logs | StudentController |
| 34 | GET | /api/v1/students/department-performance | StudentController |
| 35 | POST | /api/v1/students/{id}/make-captain | StudentController |
| 36 | POST | /api/v1/students/{id}/remove-captain | StudentController |
| 37 | GET | /api/v1/students/stages | StudentController |
| 38 | GET | /api/v1/students/subgroups/{subgroupId}/activities | StudentController |
| 39 | GET | /api/v1/group-activities/assignments/{assignmentId}/teams | GroupActivityController |
| 40 | POST | /api/v1/group-activities/teams/{teamId}/award-xp | GroupActivityController |
| 41 | POST | /api/v1/auth/login | AuthController |
| 42 | POST | /api/v1/auth/student-login | AuthController |
| 43 | GET | /api/v1/auth/me | AuthController |
| 44 | GET | /api/v1/admin/stats | AdminDashboardController |
| 45 | GET | /api/v1/admin/academic-years | AdminLookupController |
| 46 | GET | /api/v1/admin/years | AdminLookupController |
| 47 | GET | /api/v1/admin/semesters | AdminLookupController |
| 48 | GET | /api/v1/admin/genders | AdminLookupController |
| 49 | GET | /api/v1/admin/sections | AdminLookupController |
| 50 | PUT | /api/v1/admin/subgroups/{id}/assign-faculty | AdminFacultyController |
| 51 | GET | /api/v1/admin/stages | AdminStageController |
| 52 | POST | /api/v1/admin/stages | AdminStageController |
| 53 | GET | /api/v1/admin/stages/{id} | AdminStageController |
| 54 | PUT | /api/v1/admin/stages/{id} | AdminStageController |
| 55 | GET | /api/v1/admin/stages/{id}/report | AdminStageController |
| 56 | DELETE | /api/v1/admin/stages/{id} | AdminStageController |
| 57 | GET | /api/v1/admin/roles | AdminRoleController |
| 58 | POST | /api/v1/admin/roles | AdminRoleController |
| 59 | GET | /api/v1/admin/departments | AdminDepartmentController |
| 60 | POST | /api/v1/admin/departments | AdminDepartmentController |
| 61 | PUT | /api/v1/admin/departments/{id} | AdminDepartmentController |
| 62 | DELETE | /api/v1/admin/departments/{id} | AdminDepartmentController |
| 63 | GET | /api/v1/admin/departments/{id}/sections | AdminDepartmentController |
| 64 | POST | /api/v1/admin/departments/{id}/sections | AdminDepartmentController |
| 65 | DELETE | /api/v1/admin/departments/{id}/sections/{sectionId} | AdminDepartmentController |
| 66 | GET | /api/v1/admin/departments/class-coordinators | AdminDepartmentController |
| 67 | POST | /api/v1/admin/stages/{stageId}/subgroups | AdminSubgroupController |
| 68 | PUT | /api/v1/admin/subgroups/{id} | AdminSubgroupController |
| 69 | DELETE | /api/v1/admin/subgroups/{id} | AdminSubgroupController |
| 70 | GET | /api/v1/admin/subjects | AdminSubjectController |
| 71 | POST | /api/v1/admin/subjects | AdminSubjectController |
| 72 | DELETE | /api/v1/admin/subjects/{id} | AdminSubjectController |
| 73 | GET | /api/v1/admin/my-activities | AdminActivityController |
| 74 | GET | /api/v1/admin/subgroups/{subgroupId}/activities | AdminActivityController |
| 75 | POST | /api/v1/admin/subgroups/{subgroupId}/activities | AdminActivityController |
| 76 | PUT | /api/v1/admin/activities/{activityId} | AdminActivityController |
| 77 | POST | /api/v1/admin/activities/{id}/assign | AdminActivityController |
| 78 | DELETE | /api/v1/admin/activities/{activityId} | AdminActivityController |
| 79 | GET | /api/v1/admin/frequencies/custom | AdminActivityController |
| 80 | POST | /api/v1/admin/frequencies/custom | AdminActivityController |
| 81 | GET | /api/v1/admin/users | AdminUserController |
| 82 | POST | /api/v1/admin/users | AdminUserController |
| 83 | PUT | /api/v1/admin/users/{id} | AdminUserController |
| 84 | DELETE | /api/v1/admin/users/{id} | AdminUserController |
| 85 | POST | /api/v1/teams | TeamController |
| 86 | GET | /api/v1/teams | TeamController |
| 87 | GET | /api/v1/teams/my-team | TeamController |
| 88 | GET | /api/v1/teams/{id} | TeamController |
| 89 | PUT | /api/v1/teams/{id} | TeamController |
| 90 | POST | /api/v1/teams/{id}/members | TeamController |
| 91 | DELETE | /api/v1/teams/{id}/members/{studentId} | TeamController |
| 92 | POST | /api/v1/teams/{id}/captain | TeamController |
| 93 | GET | /api/v1/teams/my-classmates | TeamController |
| 94 | POST | /api/v1/teams/my-team/add-member | TeamController |
| 95 | POST | /api/v1/teams/{id}/add-member | TeamController |
| 96 | POST | /api/v1/teams/{id}/remove-member | TeamController |
| 97 | POST | /api/v1/teams/my-team/remove-request | TeamController |
| 98 | GET | /api/v1/teams/removal-requests/pending | TeamController |
| 99 | PUT | /api/v1/teams/removal-requests/{id}/approve | TeamController |
| 100 | PUT | /api/v1/teams/removal-requests/{id}/reject | TeamController |
| 101 | PUT | /api/v1/teams/{id}/limit | TeamController |
| 102 | DELETE | /api/v1/teams/{teamId} | TeamController |

# APIs Grouped By Controller

### AdminActivityController
GET /api/v1/admin/my-activities
GET /api/v1/admin/subgroups/{subgroupId}/activities
POST /api/v1/admin/subgroups/{subgroupId}/activities
PUT /api/v1/admin/activities/{activityId}
POST /api/v1/admin/activities/{id}/assign
DELETE /api/v1/admin/activities/{activityId}
GET /api/v1/admin/frequencies/custom
POST /api/v1/admin/frequencies/custom

### AdminDashboardController
GET /api/v1/admin/stats

### AdminDepartmentController
GET /api/v1/admin/departments
POST /api/v1/admin/departments
PUT /api/v1/admin/departments/{id}
DELETE /api/v1/admin/departments/{id}
GET /api/v1/admin/departments/{id}/sections
POST /api/v1/admin/departments/{id}/sections
DELETE /api/v1/admin/departments/{id}/sections/{sectionId}
GET /api/v1/admin/departments/class-coordinators

### AdminFacultyController
PUT /api/v1/admin/subgroups/{id}/assign-faculty

### AdminLookupController
GET /api/v1/admin/academic-years
GET /api/v1/admin/years
GET /api/v1/admin/semesters
GET /api/v1/admin/genders
GET /api/v1/admin/sections

### AdminRoleController
GET /api/v1/admin/roles
POST /api/v1/admin/roles

### AdminStageController
GET /api/v1/admin/stages
POST /api/v1/admin/stages
GET /api/v1/admin/stages/{id}
PUT /api/v1/admin/stages/{id}
GET /api/v1/admin/stages/{id}/report
DELETE /api/v1/admin/stages/{id}

### AdminSubgroupController
POST /api/v1/admin/stages/{stageId}/subgroups
PUT /api/v1/admin/subgroups/{id}
DELETE /api/v1/admin/subgroups/{id}

### AdminSubjectController
GET /api/v1/admin/subjects
POST /api/v1/admin/subjects
DELETE /api/v1/admin/subjects/{id}

### AdminUserController
GET /api/v1/admin/users
POST /api/v1/admin/users
PUT /api/v1/admin/users/{id}
DELETE /api/v1/admin/users/{id}

### AuthController
POST /api/v1/auth/login
POST /api/v1/auth/student-login
GET /api/v1/auth/me

### GroupActivityController
GET /api/v1/group-activities/assignments/{assignmentId}/teams
POST /api/v1/group-activities/teams/{teamId}/award-xp

### LevelBadgeController
GET /api/v1/levels
GET /api/v1/levels/student/{studentId}/current
GET /api/v1/levels/me/current
GET /api/v1/badges
GET /api/v1/badges/student/me
GET /api/v1/badges/student/{studentId}
POST /api/v1/badges/submit
PUT /api/v1/badges/{id}/approve
PUT /api/v1/badges/{id}/reject
GET /api/v1/badges/pending

### StudentController
POST /api/v1/students
GET /api/v1/students
GET /api/v1/students/{id}
GET /api/v1/students/search
DELETE /api/v1/students/{id}
PUT /api/v1/students/{id}
POST /api/v1/students/bulk-parse
POST /api/v1/students/bulk-import
POST /api/v1/students/{id}/adjust-points
GET /api/v1/students/{id}/discipline-logs
GET /api/v1/students/department-performance
POST /api/v1/students/{id}/make-captain
POST /api/v1/students/{id}/remove-captain
GET /api/v1/students/stages
GET /api/v1/students/subgroups/{subgroupId}/activities

### StudentXpController
GET /api/v1/my-activities/{activityId}/years
GET /api/v1/my-activities/{activityId}/departments
GET /api/v1/my-activities/{activityId}/sections
GET /api/v1/my-activities/{activityId}/students
POST /api/v1/student-xp/award
POST /api/v1/student-xp/award/batch

### TeamController
POST /api/v1/teams
GET /api/v1/teams
GET /api/v1/teams/my-team
GET /api/v1/teams/{id}
PUT /api/v1/teams/{id}
POST /api/v1/teams/{id}/members
DELETE /api/v1/teams/{id}/members/{studentId}
POST /api/v1/teams/{id}/captain
GET /api/v1/teams/my-classmates
POST /api/v1/teams/my-team/add-member
POST /api/v1/teams/{id}/add-member
POST /api/v1/teams/{id}/remove-member
POST /api/v1/teams/my-team/remove-request
GET /api/v1/teams/removal-requests/pending
PUT /api/v1/teams/removal-requests/{id}/approve
PUT /api/v1/teams/removal-requests/{id}/reject
PUT /api/v1/teams/{id}/limit
DELETE /api/v1/teams/{teamId}

### XpController
GET /api/v1/xp/{studentId}/summary
GET /api/v1/xp/{studentId}/history
GET /api/v1/xp/{studentId}/streaks
POST /api/v1/xp/submit
PUT /api/v1/xp/{id}/approve
PUT /api/v1/xp/{id}/reject
POST /api/v1/xp/penalty

# Project Statistics
- **Total Controllers:** 17
- **Total APIs:** 102
- **GET:** 45
- **POST:** 32
- **PUT:** 15
- **PATCH:** 0
- **DELETE:** 10
- **Public APIs:** 2
- **Protected APIs:** 100
