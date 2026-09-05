package jjcet.PragatiX.modules.enrollment;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.enums.DepartmentType;
import jjcet.PragatiX.modules.activity.repository.ActivityStageRepository;
import jjcet.PragatiX.modules.audit.service.AuditService;
import jjcet.PragatiX.modules.authentication.repository.UserRepository;
import jjcet.PragatiX.modules.enrollment.dto.CompleteEnrollmentResponseDto;
import jjcet.PragatiX.modules.enrollment.dto.EnrollmentDto;
import jjcet.PragatiX.modules.enrollment.dto.SingleEnrollmentRequestDto;
import jjcet.PragatiX.modules.enrollment.entity.Enrollment;
import jjcet.PragatiX.modules.enrollment.entity.EnrollmentSetting;
import jjcet.PragatiX.modules.enrollment.enums.EnrollmentStatus;
import jjcet.PragatiX.modules.enrollment.repository.EnrollmentRepository;
import jjcet.PragatiX.modules.enrollment.repository.EnrollmentSettingRepository;
import jjcet.PragatiX.modules.enrollment.service.EnrollmentService;
import jjcet.PragatiX.modules.student.dto.request.CreateStudentRequest;
import jjcet.PragatiX.modules.student.dto.request.UpdateStudentRequest;
import jjcet.PragatiX.modules.student.dto.response.StudentResponse;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.modules.student.service.StudentCommandService;
import jjcet.PragatiX.modules.student.service.StudentLookupService;
import jjcet.PragatiX.modules.student.service.StudentMapper;
import jjcet.PragatiX.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EnrollmentSectionComprehensiveTest {

    // Enrollment dependencies
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private EnrollmentSettingRepository enrollmentSettingRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private SectionRepository sectionRepository;
    @Mock private GenderRepository genderRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private YearRepository yearRepository;
    @Mock private SemesterRepository semesterRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ActivityStageRepository activityStageRepository;
    @Mock private AuditService auditService;
    @Mock private PasswordEncoder passwordEncoder;

    // Student dependencies
    @Mock private TeamRepository teamRepository;
    @Mock private UserRepository userRepository;
    @Mock private StudentLookupService studentLookupService;
    @Mock private StudentMapper studentMapper;
    @Mock private StudentGuardianRepository studentGuardianRepository;
    @Mock private jjcet.PragatiX.admin.service.TeamCleanupService teamCleanupService;

    private EnrollmentService enrollmentService;
    private StudentCommandService studentCommandService;

    private Department cseDept;
    private Department civilDept;
    private Department tamilDept;
    private Section sectionA;
    private Section sectionB;
    private Gender maleGender;
    private Year year1;
    private Semester semester1;
    private EnrollmentSetting enrollmentSetting;
    private User superAdminUser;

    @BeforeEach
    void setUp() {
        enrollmentService = new EnrollmentService(
                enrollmentRepository,
                enrollmentSettingRepository,
                departmentRepository,
                sectionRepository,
                genderRepository,
                academicYearRepository,
                yearRepository,
                semesterRepository,
                studentRepository,
                activityStageRepository,
                auditService,
                passwordEncoder
        );

        studentCommandService = new StudentCommandService(
                passwordEncoder,
                studentRepository,
                teamRepository,
                userRepository,
                studentLookupService,
                studentMapper,
                studentGuardianRepository,
                teamCleanupService,
                activityStageRepository,
                auditService
        );

        cseDept = Department.builder()
                .name("Computer Science and Engineering")
                .code("CSE")
                .deptCode("CSE")
                .departmentType(DepartmentType.MAIN)
                .build();
        cseDept.setId(1L);

        civilDept = Department.builder()
                .name("Civil Engineering")
                .code("CIVIL")
                .deptCode("CIVIL")
                .departmentType(DepartmentType.MAIN)
                .build();
        civilDept.setId(2L);

        tamilDept = Department.builder()
                .name("Department of Tamil")
                .code("TAMIL")
                .deptCode("TAMIL")
                .departmentType(DepartmentType.SUB)
                .build();
        tamilDept.setId(3L);

        sectionA = Section.builder().department(cseDept).sectionName("A").build();
        sectionA.setId(10L);
        sectionB = Section.builder().department(cseDept).sectionName("B").build();
        sectionB.setId(11L);

        maleGender = Gender.builder().genderName("Male").build();
        maleGender.setId(1L);

        year1 = new Year();
        year1.setId(1L);
        year1.setYearNo((byte) 1);
        year1.setYearName("Year 1");

        semester1 = new Semester();
        semester1.setId(1L);
        semester1.setSemesterNo((byte) 1);
        semester1.setSemesterName("Semester 1");

        enrollmentSetting = new EnrollmentSetting();
        enrollmentSetting.setId(1L);
        enrollmentSetting.setEnrollmentEnabled(true);

        Role superAdminRole = new Role();
        superAdminRole.setName("ROLE_SUPER_ADMIN");
        superAdminUser = new User();
        superAdminUser.setUsername("superadmin");
        superAdminUser.setRoles(Collections.singleton(superAdminRole));
        superAdminUser.setSubRoles(Collections.emptySet());
    }

    // =========================================================================
    // Test A: Enrollment with Department + existing Section
    // =========================================================================
    @Test
    @DisplayName("A. Enrollment with Department + existing Section")
    void testA_EnrollmentWithExistingSection() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(cseDept));
        when(sectionRepository.findByDepartmentAndSectionName(cseDept, "A"))
                .thenReturn(Optional.of(sectionA));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> {
            Enrollment e = i.getArgument(0);
            e.setId(101L);
            return e;
        });

        SingleEnrollmentRequestDto request = new SingleEnrollmentRequestDto();
        request.setFullName("Alice");
        request.setEmail("alice@test.com");
        request.setMobile("9876543210");
        request.setDepartmentId(1L);
        request.setGender("Male");
        request.setSection("A");

        EnrollmentDto result = enrollmentService.createSingleEnrollment(request, "admin");

        assertNotNull(result);
        assertEquals(10L, result.getSectionId());
        assertEquals("A", result.getSectionName());

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        Enrollment saved = captor.getValue();
        assertNotNull(saved.getSection());
        assertEquals("A", saved.getSection().getSectionName());
        assertEquals(cseDept, saved.getSection().getDepartment());
        verify(sectionRepository, never()).save(any(Section.class)); // No duplicate creation
    }

    // =========================================================================
    // Test B: Enrollment with Department + new Section
    // =========================================================================
    @Test
    @DisplayName("B. Enrollment with Department + new Section (auto-creates section)")
    void testB_EnrollmentWithNewSection() {
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(civilDept));
        when(sectionRepository.findByDepartmentAndSectionName(civilDept, "C")).thenReturn(Optional.empty());

        Section newSection = Section.builder().department(civilDept).sectionName("C").build();
        newSection.setId(99L);
        when(sectionRepository.save(any(Section.class))).thenReturn(newSection);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> {
            Enrollment e = i.getArgument(0);
            e.setId(102L);
            return e;
        });

        SingleEnrollmentRequestDto request = new SingleEnrollmentRequestDto();
        request.setFullName("Bob");
        request.setEmail("bob@test.com");
        request.setMobile("9876543211");
        request.setDepartmentId(2L);
        request.setGender("Male");
        request.setSection("C");

        EnrollmentDto result = enrollmentService.createSingleEnrollment(request, "admin");

        assertNotNull(result);
        assertEquals(99L, result.getSectionId());
        assertEquals("C", result.getSectionName());

        verify(sectionRepository, times(1)).save(argThat(s ->
                s.getSectionName().equals("C") && s.getDepartment().equals(civilDept)
        ));
    }

    // =========================================================================
    // Test C: Enrollment with Department + no Section
    // =========================================================================
    @Test
    @DisplayName("C. Enrollment with Department + no Section")
    void testC_EnrollmentWithoutSection() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(cseDept));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> {
            Enrollment e = i.getArgument(0);
            e.setId(103L);
            return e;
        });

        SingleEnrollmentRequestDto request = new SingleEnrollmentRequestDto();
        request.setFullName("Charlie");
        request.setEmail("charlie@test.com");
        request.setMobile("9876543212");
        request.setDepartmentId(1L);
        request.setGender("Male");
        request.setSection(null); // No section

        EnrollmentDto result = enrollmentService.createSingleEnrollment(request, "admin");

        assertNotNull(result);
        assertNull(result.getSectionId());
        assertNull(result.getSectionName());

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(captor.capture());
        assertNull(captor.getValue().getSection());
        verify(sectionRepository, never()).save(any(Section.class));
    }

    // =========================================================================
    // Test D: Single Student Add with Section
    // =========================================================================
    @Test
    @DisplayName("D. Single Student Add with Section")
    void testD_SingleStudentAddWithSection() {
        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.of(superAdminUser));
        when(studentLookupService.resolveDepartment(1L, "Computer Science and Engineering")).thenReturn(cseDept);
        when(studentLookupService.resolveYear(1L, "Year 1")).thenReturn(year1);
        when(studentLookupService.resolveSemester(1L, "Semester 1")).thenReturn(semester1);
        when(studentLookupService.resolveGender(1L, "MALE")).thenReturn(maleGender);
        when(studentLookupService.resolveSection(eq(10L), eq("A"), eq(cseDept))).thenReturn(sectionA);

        when(studentRepository.save(any(Student.class))).thenAnswer(i -> {
            Student s = i.getArgument(0);
            s.setId(100L);
            return s;
        });
        when(studentMapper.toResponse(any(Student.class), any())).thenAnswer(i -> {
            Student s = i.getArgument(0);
            StudentResponse resp = new StudentResponse();
            resp.setId(s.getId());
            resp.setFullName(s.getFullName());
            resp.setSection(s.getSection() != null ? s.getSection().getSectionName() : null);
            return resp;
        });

        CreateStudentRequest request = new CreateStudentRequest();
        request.setFullName("David");
        request.setRegNo("81130001");
        request.setSprNo("SPR001");
        request.setEmail("student1@test.com");
        request.setPhone("9876543213");
        request.setDepartmentName("Computer Science and Engineering");
        request.setDepartmentId(1L);
        request.setYear("Year 1");
        request.setYearId(1L);
        request.setSemester("Semester 1");
        request.setSemesterId(1L);
        request.setGender("MALE");
        request.setGenderId(1L);
        request.setSection("A");
        request.setSectionId(10L);

        ApiResponse<StudentResponse> response = studentCommandService.createStudent(request, "superadmin");

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("A", response.getData().getSection());

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertEquals(sectionA, captor.getValue().getSection());
    }

    // =========================================================================
    // Test E: Single Student Add without Section
    // =========================================================================
    @Test
    @DisplayName("E. Single Student Add without Section")
    void testE_SingleStudentAddWithoutSection() {
        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.of(superAdminUser));
        when(studentLookupService.resolveDepartment(1L, "Computer Science and Engineering")).thenReturn(cseDept);
        when(studentLookupService.resolveYear(1L, "Year 1")).thenReturn(year1);
        when(studentLookupService.resolveSemester(1L, "Semester 1")).thenReturn(semester1);
        when(studentLookupService.resolveGender(1L, "MALE")).thenReturn(maleGender);
        when(studentLookupService.resolveSection(isNull(), isNull(), eq(cseDept))).thenReturn(null);

        when(studentRepository.save(any(Student.class))).thenAnswer(i -> {
            Student s = i.getArgument(0);
            s.setId(101L);
            return s;
        });
        when(studentMapper.toResponse(any(Student.class), any())).thenAnswer(i -> {
            Student s = i.getArgument(0);
            StudentResponse resp = new StudentResponse();
            resp.setId(s.getId());
            resp.setFullName(s.getFullName());
            resp.setSection(null);
            return resp;
        });

        CreateStudentRequest request = new CreateStudentRequest();
        request.setFullName("Emma");
        request.setRegNo("81130002");
        request.setSprNo("SPR002");
        request.setEmail("student2@test.com");
        request.setPhone("9876543214");
        request.setDepartmentName("Computer Science and Engineering");
        request.setDepartmentId(1L);
        request.setYear("Year 1");
        request.setYearId(1L);
        request.setSemester("Semester 1");
        request.setSemesterId(1L);
        request.setGender("MALE");
        request.setGenderId(1L);
        request.setSection(null);
        request.setSectionId(null);

        ApiResponse<StudentResponse> response = studentCommandService.createStudent(request, "superadmin");

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNull(response.getData().getSection());

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(captor.capture());
        assertNull(captor.getValue().getSection());
    }

    // =========================================================================
    // Test F: Bulk Student upload with Section
    // =========================================================================
    @Test
    @DisplayName("F. Bulk Student upload with Section")
    void testF_BulkUploadWithSection() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Enrollment Template");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Sl.No.");
        header.createCell(1).setCellValue("Student Name");
        header.createCell(2).setCellValue("Gender");
        header.createCell(3).setCellValue("Email ID");
        header.createCell(4).setCellValue("Mobile Number");
        header.createCell(5).setCellValue("Branch / Department");
        header.createCell(6).setCellValue("Section");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue(1);
        row.createCell(1).setCellValue("Frank");
        row.createCell(2).setCellValue("Male");
        row.createCell(3).setCellValue("frank@test.com");
        row.createCell(4).setCellValue("9876543215");
        row.createCell(5).setCellValue("Computer Science and Engineering");
        row.createCell(6).setCellValue("B");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        MockMultipartFile file = new MockMultipartFile("file", "enrollment.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bos.toByteArray());

        when(departmentRepository.findAll()).thenReturn(Collections.singletonList(cseDept));
        when(sectionRepository.findByDepartmentAndSectionName(cseDept, "B")).thenReturn(Optional.of(sectionB));
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        var importResult = enrollmentService.importEnrollmentExcel(file, "admin");

        assertEquals(1, importResult.getImportedCount());
        assertEquals(0, importResult.getSkippedCount());

        ArgumentCaptor<List<Enrollment>> captor = ArgumentCaptor.forClass(List.class);
        verify(enrollmentRepository).saveAll(captor.capture());
        List<Enrollment> savedList = captor.getValue();
        assertEquals(1, savedList.size());
        assertNotNull(savedList.get(0).getSection());
        assertEquals("B", savedList.get(0).getSection().getSectionName());
    }

    // =========================================================================
    // Test G: Bulk Student upload without Section
    // =========================================================================
    @Test
    @DisplayName("G. Bulk Student upload without Section")
    void testG_BulkUploadWithoutSection() throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Enrollment Template");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Sl.No.");
        header.createCell(1).setCellValue("Student Name");
        header.createCell(2).setCellValue("Gender");
        header.createCell(3).setCellValue("Email ID");
        header.createCell(4).setCellValue("Mobile Number");
        header.createCell(5).setCellValue("Branch / Department");
        header.createCell(6).setCellValue("Section");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue(1);
        row.createCell(1).setCellValue("Grace");
        row.createCell(2).setCellValue("Male");
        row.createCell(3).setCellValue("grace@test.com");
        row.createCell(4).setCellValue("9876543216");
        row.createCell(5).setCellValue("Computer Science and Engineering");
        row.createCell(6).setCellValue(""); // Empty section

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        MockMultipartFile file = new MockMultipartFile("file", "enrollment.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bos.toByteArray());

        when(departmentRepository.findAll()).thenReturn(Collections.singletonList(cseDept));
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        var importResult = enrollmentService.importEnrollmentExcel(file, "admin");

        assertEquals(1, importResult.getImportedCount());
        assertEquals(0, importResult.getSkippedCount());

        ArgumentCaptor<List<Enrollment>> captor = ArgumentCaptor.forClass(List.class);
        verify(enrollmentRepository).saveAll(captor.capture());
        List<Enrollment> savedList = captor.getValue();
        assertEquals(1, savedList.size());
        assertNull(savedList.get(0).getSection()); // Optional section correctly null
    }

    // =========================================================================
    // Test H: Edit student and add Section
    // =========================================================================
    @Test
    @DisplayName("H. Edit student and add Section")
    void testH_EditStudentAddSection() {
        Student student = Student.builder()
                .fullName("Harry Potter")
                .regNo("81131001")
                .sprNo("SPR1001")
                .department(cseDept)
                .yearRef(year1)
                .semesterRef(semester1)
                .genderRef(maleGender)
                .section(null) // Initially no section
                .build();
        student.setId(201L);

        when(studentRepository.findById(201L)).thenReturn(Optional.of(student));
        when(studentLookupService.resolveDepartment(isNull(), isNull())).thenReturn(cseDept);
        when(studentLookupService.resolveYear(isNull(), isNull())).thenReturn(year1);
        when(studentLookupService.resolveSemester(isNull(), isNull())).thenReturn(semester1);
        when(studentLookupService.resolveGender(isNull(), isNull())).thenReturn(maleGender);
        when(studentLookupService.resolveSection(eq(10L), eq("A"), eq(cseDept))).thenReturn(sectionA);
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
        when(studentMapper.toResponse(any(Student.class), any())).thenAnswer(i -> {
            Student s = i.getArgument(0);
            StudentResponse resp = new StudentResponse();
            resp.setId(s.getId());
            resp.setSection(s.getSection() != null ? s.getSection().getSectionName() : null);
            return resp;
        });

        UpdateStudentRequest request = new UpdateStudentRequest();
        request.setFullName("Harry Potter");
        request.setSectionId(10L);
        request.setSection("A");

        ApiResponse<StudentResponse> response = studentCommandService.updateStudent(201L, request);

        assertTrue(response.isSuccess());
        assertEquals(sectionA, student.getSection());
    }

    // =========================================================================
    // Test I: Edit student and change Section
    // =========================================================================
    @Test
    @DisplayName("I. Edit student and change Section")
    void testI_EditStudentChangeSection() {
        Student student = Student.builder()
                .fullName("Isabella Swan")
                .regNo("81131002")
                .sprNo("SPR1002")
                .department(cseDept)
                .yearRef(year1)
                .semesterRef(semester1)
                .genderRef(maleGender)
                .section(sectionA) // Currently Section A
                .build();
        student.setId(202L);

        when(studentRepository.findById(202L)).thenReturn(Optional.of(student));
        when(studentLookupService.resolveDepartment(isNull(), isNull())).thenReturn(cseDept);
        when(studentLookupService.resolveYear(isNull(), isNull())).thenReturn(year1);
        when(studentLookupService.resolveSemester(isNull(), isNull())).thenReturn(semester1);
        when(studentLookupService.resolveGender(isNull(), isNull())).thenReturn(maleGender);
        when(studentLookupService.resolveSection(eq(11L), eq("B"), eq(cseDept))).thenReturn(sectionB);
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
        when(studentMapper.toResponse(any(Student.class), any())).thenAnswer(i -> {
            Student s = i.getArgument(0);
            StudentResponse resp = new StudentResponse();
            resp.setId(s.getId());
            resp.setSection(s.getSection() != null ? s.getSection().getSectionName() : null);
            return resp;
        });

        UpdateStudentRequest request = new UpdateStudentRequest();
        request.setFullName("Isabella Swan");
        request.setSectionId(11L);
        request.setSection("B");

        ApiResponse<StudentResponse> response = studentCommandService.updateStudent(202L, request);

        assertTrue(response.isSuccess());
        assertEquals(sectionB, student.getSection());
    }

    // =========================================================================
    // Test J: Edit student and remove Section
    // =========================================================================
    @Test
    @DisplayName("J. Edit student and remove Section")
    void testJ_EditStudentRemoveSection() {
        Student student = Student.builder()
                .fullName("Jack Sparrow")
                .regNo("81131003")
                .sprNo("SPR1003")
                .department(cseDept)
                .yearRef(year1)
                .semesterRef(semester1)
                .genderRef(maleGender)
                .section(sectionA) // Currently Section A
                .build();
        student.setId(203L);

        when(studentRepository.findById(203L)).thenReturn(Optional.of(student));
        when(studentLookupService.resolveDepartment(isNull(), isNull())).thenReturn(cseDept);
        when(studentLookupService.resolveYear(isNull(), isNull())).thenReturn(year1);
        when(studentLookupService.resolveSemester(isNull(), isNull())).thenReturn(semester1);
        when(studentLookupService.resolveGender(isNull(), isNull())).thenReturn(maleGender);
        when(studentLookupService.resolveSection(isNull(), isNull(), eq(cseDept))).thenReturn(null);
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
        when(studentMapper.toResponse(any(Student.class), any())).thenAnswer(i -> {
            Student s = i.getArgument(0);
            StudentResponse resp = new StudentResponse();
            resp.setId(s.getId());
            resp.setSection(s.getSection() != null ? s.getSection().getSectionName() : null);
            return resp;
        });

        UpdateStudentRequest request = new UpdateStudentRequest();
        request.setFullName("Jack Sparrow");
        request.setSectionId(null);
        request.setSection(null);

        ApiResponse<StudentResponse> response = studentCommandService.updateStudent(203L, request);

        assertTrue(response.isSuccess());
        assertNull(student.getSection()); // Successfully removed
    }

    // =========================================================================
    // Test K: Verify automatic Section creation
    // =========================================================================
    @Test
    @DisplayName("K. Verify automatic Section creation under Department")
    void testK_VerifyAutomaticSectionCreation() {
        Department aidsDept = Department.builder()
                .name("Artificial Intelligence and Data Science")
                .code("AIDS")
                .deptCode("AIDS")
                .departmentType(DepartmentType.MAIN)
                .build();
        aidsDept.setId(4L);

        when(departmentRepository.findById(4L)).thenReturn(Optional.of(aidsDept));
        when(sectionRepository.findByDepartmentAndSectionName(aidsDept, "AI-A")).thenReturn(Optional.empty());

        Section newSec = Section.builder().department(aidsDept).sectionName("AI-A").build();
        newSec.setId(50L);
        when(sectionRepository.save(any(Section.class))).thenReturn(newSec);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> {
            Enrollment e = i.getArgument(0);
            e.setId(104L);
            return e;
        });

        SingleEnrollmentRequestDto request = new SingleEnrollmentRequestDto();
        request.setFullName("Kevin");
        request.setEmail("kevin@test.com");
        request.setMobile("9876543217");
        request.setDepartmentId(4L);
        request.setGender("Male");
        request.setSection("AI-A");

        EnrollmentDto result = enrollmentService.createSingleEnrollment(request, "admin");

        assertEquals(50L, result.getSectionId());
        assertEquals("AI-A", result.getSectionName());

        verify(sectionRepository).save(argThat(s ->
                s.getSectionName().equals("AI-A") && s.getDepartment().equals(aidsDept)
        ));
    }

    // =========================================================================
    // Test L: Verify no duplicate Section creation
    // =========================================================================
    @Test
    @DisplayName("L. Verify no duplicate Section creation when same Section name exists")
    void testL_VerifyNoDuplicateSectionCreation() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(cseDept));
        when(sectionRepository.findByDepartmentAndSectionName(cseDept, "A")).thenReturn(Optional.of(sectionA));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(i -> {
            Enrollment e = i.getArgument(0);
            e.setId(105L);
            return e;
        });

        SingleEnrollmentRequestDto req1 = new SingleEnrollmentRequestDto();
        req1.setFullName("Student One");
        req1.setEmail("s1@test.com");
        req1.setMobile("9876543218");
        req1.setDepartmentId(1L);
        req1.setGender("Male");
        req1.setSection("A");

        SingleEnrollmentRequestDto req2 = new SingleEnrollmentRequestDto();
        req2.setFullName("Student Two");
        req2.setEmail("s2@test.com");
        req2.setMobile("9876543219");
        req2.setDepartmentId(1L);
        req2.setGender("Male");
        req2.setSection("a"); // Lowercase "a" matches existing "A" case-insensitively

        enrollmentService.createSingleEnrollment(req1, "admin");
        enrollmentService.createSingleEnrollment(req2, "admin");

        verify(sectionRepository, never()).save(any(Section.class)); // Reused existing section
    }

    // =========================================================================
    // Test M: Verify Year 1 + Semester 1 assignment on completeEnrollment
    // =========================================================================
    @Test
    @DisplayName("M. Verify Year 1 + Semester 1 assignment")
    void testM_VerifyYear1AndSemester1Assignment() {
        Enrollment enrollmentWithSec = new Enrollment();
        enrollmentWithSec.setId(1L);
        enrollmentWithSec.setFullName("Mary");
        enrollmentWithSec.setEmail("mary@test.com");
        enrollmentWithSec.setMobile("9876543220");
        enrollmentWithSec.setGender("Male");
        enrollmentWithSec.setDepartment(cseDept);
        enrollmentWithSec.setSection(sectionA);
        enrollmentWithSec.setStatus(EnrollmentStatus.PENDING);

        Enrollment enrollmentWithoutSec = new Enrollment();
        enrollmentWithoutSec.setId(2L);
        enrollmentWithoutSec.setFullName("Nick");
        enrollmentWithoutSec.setEmail("nick@test.com");
        enrollmentWithoutSec.setMobile("9876543221");
        enrollmentWithoutSec.setGender("Male");
        enrollmentWithoutSec.setDepartment(civilDept);
        enrollmentWithoutSec.setSection(null);
        enrollmentWithoutSec.setStatus(EnrollmentStatus.PENDING);

        when(enrollmentSettingRepository.findAll()).thenReturn(Collections.singletonList(enrollmentSetting));
        when(enrollmentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(enrollmentWithSec));
        when(enrollmentRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(enrollmentWithoutSec));
        when(genderRepository.findByGenderName("Male")).thenReturn(Optional.of(maleGender));
        when(yearRepository.findByYearNo((byte) 1)).thenReturn(Optional.of(year1));
        when(semesterRepository.findBySemesterNo((byte) 1)).thenReturn(Optional.of(semester1));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> {
            Student s = i.getArgument(0);
            s.setId(500L);
            return s;
        });

        // 1. Complete enrollment for student WITH section
        CompleteEnrollmentResponseDto res1 = enrollmentService.completeEnrollment(1L);
        assertTrue(res1.isSuccess());

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository, times(1)).save(captor.capture());
        Student saved1 = captor.getValue();
        assertEquals(1, (int) saved1.getYearRef().getYearNo());
        assertEquals(1, (int) saved1.getSemesterRef().getSemesterNo());
        assertEquals("1", saved1.getYear());
        assertEquals("1", saved1.getSemester());
        assertEquals(sectionA, saved1.getSection());

        // 2. Complete enrollment for student WITHOUT section
        CompleteEnrollmentResponseDto res2 = enrollmentService.completeEnrollment(2L);
        assertTrue(res2.isSuccess());

        verify(studentRepository, times(2)).save(captor.capture());
        Student saved2 = captor.getValue();
        assertEquals(1, (int) saved2.getYearRef().getYearNo());
        assertEquals(1, (int) saved2.getSemesterRef().getSemesterNo());
        assertEquals("1", saved2.getYear());
        assertEquals("1", saved2.getSemester());
        assertNull(saved2.getSection());
    }

    // =========================================================================
    // Test N: Verify existing students without Section still work
    // =========================================================================
    @Test
    @DisplayName("N. Verify existing students without Section still work")
    void testN_ExistingStudentsWithoutSectionStillWork() {
        Student existingStudent = Student.builder()
                .fullName("Olivia Wild")
                .regNo("81139999")
                .sprNo("SPR999")
                .email("olivia@test.com")
                .phoneNo("9876543222")
                .department(cseDept)
                .yearRef(year1)
                .semesterRef(semester1)
                .genderRef(maleGender)
                .section(null) // Existing student with no section
                .build();
        existingStudent.setId(300L);

        when(studentRepository.findById(300L)).thenReturn(Optional.of(existingStudent));
        when(studentLookupService.resolveDepartment(isNull(), isNull())).thenReturn(cseDept);
        when(studentLookupService.resolveYear(isNull(), isNull())).thenReturn(year1);
        when(studentLookupService.resolveSemester(isNull(), isNull())).thenReturn(semester1);
        when(studentLookupService.resolveGender(isNull(), isNull())).thenReturn(maleGender);
        when(studentLookupService.resolveSection(isNull(), isNull(), eq(cseDept))).thenReturn(null);
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));
        when(studentMapper.toResponse(any(Student.class), any())).thenAnswer(i -> {
            Student s = i.getArgument(0);
            StudentResponse resp = new StudentResponse();
            resp.setId(s.getId());
            resp.setPhone(s.getPhoneNo());
            resp.setSection(s.getSection() != null ? s.getSection().getSectionName() : null);
            return resp;
        });

        // Update name/phone only; section remains null
        UpdateStudentRequest request = new UpdateStudentRequest();
        request.setFullName("Olivia Wild");
        request.setPhone("9876543299");

        ApiResponse<StudentResponse> response = studentCommandService.updateStudent(300L, request);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNull(response.getData().getSection());
        assertEquals("9876543299", existingStudent.getPhoneNo());
        assertNull(existingStudent.getSection());
    }

    // =========================================================================
    // Extra Test: Department rule for non-main science departments
    // =========================================================================
    @Test
    @DisplayName("Extra: Non-main departments (e.g. Tamil) do not use Sections and throw error")
    void testDepartmentRule_NonMainDepartmentsDoNotUseSection() {
        when(departmentRepository.findById(3L)).thenReturn(Optional.of(tamilDept));

        SingleEnrollmentRequestDto request = new SingleEnrollmentRequestDto();
        request.setFullName("Paul");
        request.setEmail("paul@test.com");
        request.setMobile("9876543223");
        request.setDepartmentId(3L);
        request.setGender("Male");
        request.setSection("A");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                enrollmentService.createSingleEnrollment(request, "admin")
        );

        assertTrue(ex.getMessage().contains("Only the 9 main engineering departments are supported"));
        verify(sectionRepository, never()).save(any(Section.class));
    }

    // =========================================================================
    // Test O: Verify Enrollment Excel Template is valid and contains Section column
    // =========================================================================
    @Test
    @DisplayName("O. Verify generated Excel template is valid XLSX and has Section column")
    void testO_GenerateExcelTemplateIsValidXlsx() throws IOException {
        when(departmentRepository.findByDepartmentTypeAndDeletedFalse(jjcet.PragatiX.enums.DepartmentType.MAIN))
                .thenReturn(Arrays.asList(cseDept, civilDept));

        byte[] excelBytes = enrollmentService.generateExcelTemplate();
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(excelBytes))) {
            assertEquals(2, wb.getNumberOfSheets());
            Sheet mainSheet = wb.getSheet("Student_Enrollment");
            assertNotNull(mainSheet);
            Row headerRow = mainSheet.getRow(0);
            assertNotNull(headerRow);
            assertEquals(7, headerRow.getLastCellNum());
            assertEquals("Sl.No.", headerRow.getCell(0).getStringCellValue());
            assertEquals("Name", headerRow.getCell(1).getStringCellValue());
            assertEquals("Gender", headerRow.getCell(2).getStringCellValue());
            assertEquals("Email", headerRow.getCell(3).getStringCellValue());
            assertEquals("Mobile", headerRow.getCell(4).getStringCellValue());
            assertEquals("Branch", headerRow.getCell(5).getStringCellValue());
            assertTrue(headerRow.getCell(6).getStringCellValue().startsWith("Section"));
        }
    }

    @Test
    @DisplayName("P. Verify duplicate mobile numbers are allowed across enrollments and student creation")
    void testP_AllowDuplicateMobileNumberInEnrollmentAndCompletion() {
        SingleEnrollmentRequestDto req1 = new SingleEnrollmentRequestDto();
        req1.setFullName("Student One");
        req1.setEmail("student1@example.com");
        req1.setMobile("9876543210");
        req1.setDepartmentId(1L);

        SingleEnrollmentRequestDto req2 = new SingleEnrollmentRequestDto();
        req2.setFullName("Student Two");
        req2.setEmail("student2@example.com");
        req2.setMobile("9876543210"); // Same mobile number
        req2.setDepartmentId(1L);

        when(departmentRepository.findById(1L)).thenReturn(Optional.of(cseDept));
        when(enrollmentRepository.existsByEmailAndDeletedFalse("student1@example.com")).thenReturn(false);
        when(enrollmentRepository.existsByEmailAndDeletedFalse("student2@example.com")).thenReturn(false);
        when(studentRepository.existsByEmail("student1@example.com")).thenReturn(false);
        when(studentRepository.existsByEmail("student2@example.com")).thenReturn(false);

        Enrollment saved1 = new Enrollment();
        saved1.setId(101L);
        saved1.setFullName("Student One");
        saved1.setEmail("student1@example.com");
        saved1.setMobile("9876543210");
        saved1.setDepartment(cseDept);

        Enrollment saved2 = new Enrollment();
        saved2.setId(102L);
        saved2.setFullName("Student Two");
        saved2.setEmail("student2@example.com");
        saved2.setMobile("9876543210");
        saved2.setDepartment(cseDept);

        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(saved1).thenReturn(saved2);

        EnrollmentDto dto1 = enrollmentService.createSingleEnrollment(req1, "ADMIN");
        EnrollmentDto dto2 = enrollmentService.createSingleEnrollment(req2, "ADMIN");

        assertNotNull(dto1);
        assertNotNull(dto2);
        assertEquals("9876543210", dto1.getMobile());
        assertEquals("9876543210", dto2.getMobile());
    }
}

