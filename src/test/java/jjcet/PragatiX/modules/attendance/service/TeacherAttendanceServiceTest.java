package jjcet.PragatiX.modules.attendance.service;

import jjcet.PragatiX.entity.Attendance;
import jjcet.PragatiX.entity.AttendanceRecord;
import jjcet.PragatiX.entity.Faculty;
import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.Year;
import jjcet.PragatiX.modules.academiccalendar.service.AcademicCalendarResolver;
import jjcet.PragatiX.modules.attendance.dto.request.SaveAttendanceRequest;
import jjcet.PragatiX.modules.attendance.repository.AttendanceRepository;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.modules.faculty.repository.FacultyRepository;
import jjcet.PragatiX.modules.notification.service.NotificationService;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import jjcet.PragatiX.repository.YearRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TeacherAttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private YearRepository yearRepository;

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private AttendanceStreakService streakService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuthUtils authUtils;

    @Mock
    private AcademicCalendarResolver academicCalendarResolver;

    @Mock
    private jjcet.PragatiX.modules.authentication.repository.UserRepository userRepository;

    @Mock
    private jjcet.PragatiX.repository.DepartmentRepository departmentRepository;

    @Mock
    private jjcet.PragatiX.repository.SectionRepository sectionRepository;

    @Mock
    private jjcet.PragatiX.modules.attendance.repository.AttendanceSessionRepository attendanceSessionRepository;

    @Mock
    private jjcet.PragatiX.repository.AcademicYearRepository academicYearRepository;

    @InjectMocks
    private TeacherAttendanceService teacherAttendanceService;

    private Year year;
    private Faculty faculty;

    @BeforeEach
    void setUp() {
        year = new Year();
        year.setId(1L);
        year.setYearNo((byte) 3);

        faculty = new Faculty();
        faculty.setId(10L);
    }

    @Test
    void testSaveAttendance_AbsentTriggersSms() {
        LocalDate date = LocalDate.of(2026, 8, 21);
        Student student = Student.builder().fullName("Arun Kumar").regNo("920421104001").build();
        student.setId(101L);

        when(yearRepository.findById(1L)).thenReturn(Optional.of(year));
        when(academicCalendarResolver.isHoliday(any(), any())).thenReturn(false);
        when(facultyRepository.findByUserUsername("teacher1")).thenReturn(Optional.of(faculty));
        when(studentRepository.findById(101L)).thenReturn(Optional.of(student));
        when(attendanceRepository.findByStudentIdAndAttendanceDateAndPeriodNo(101L, date, 1))
                .thenReturn(Optional.empty());

        SaveAttendanceRequest request = new SaveAttendanceRequest();
        request.setYearId(1L);
        request.setDate(date);
        request.setPeriod(1);

        SaveAttendanceRequest.StudentAttendanceRequest record = new SaveAttendanceRequest.StudentAttendanceRequest();
        record.setStudentId(101L);
        record.setStatus(AttendanceRecord.AttendanceStatus.ABSENT);
        request.setRecords(List.of(record));

        teacherAttendanceService.saveAttendance("teacher1", request);

        verify(attendanceRepository, times(1)).save(any(Attendance.class));
        verify(notificationService, times(1)).sendAbsenceNotification(101L, date, 1);
    }

    @Test
    void testSaveAttendance_PresentDoesNotTriggerSms() {
        LocalDate date = LocalDate.of(2026, 8, 21);
        Student student = Student.builder().fullName("Arun Kumar").regNo("920421104001").build();
        student.setId(101L);

        when(yearRepository.findById(1L)).thenReturn(Optional.of(year));
        when(academicCalendarResolver.isHoliday(any(), any())).thenReturn(false);
        when(facultyRepository.findByUserUsername("teacher1")).thenReturn(Optional.of(faculty));
        when(studentRepository.findById(101L)).thenReturn(Optional.of(student));
        when(attendanceRepository.findByStudentIdAndAttendanceDateAndPeriodNo(101L, date, 1))
                .thenReturn(Optional.empty());

        SaveAttendanceRequest request = new SaveAttendanceRequest();
        request.setYearId(1L);
        request.setDate(date);
        request.setPeriod(1);

        SaveAttendanceRequest.StudentAttendanceRequest record = new SaveAttendanceRequest.StudentAttendanceRequest();
        record.setStudentId(101L);
        record.setStatus(AttendanceRecord.AttendanceStatus.PRESENT);
        request.setRecords(List.of(record));

        teacherAttendanceService.saveAttendance("teacher1", request);

        verify(attendanceRepository, times(1)).save(any(Attendance.class));
        verify(notificationService, never()).sendAbsenceNotification(anyLong(), any(), any());
    }

    @Test
    void testSaveAttendance_AbsentToAbsent_DoesNotTriggerDuplicateSms() {
        LocalDate date = LocalDate.of(2026, 8, 21);
        Student student = Student.builder().fullName("Arun Kumar").regNo("920421104001").build();
        student.setId(101L);

        Attendance existingAbsent = Attendance.builder()
                .student(student)
                .status(Attendance.AttendanceStatus.ABSENT)
                .attendanceDate(date)
                .periodNo(1)
                .build();

        when(yearRepository.findById(1L)).thenReturn(Optional.of(year));
        when(academicCalendarResolver.isHoliday(any(), any())).thenReturn(false);
        when(facultyRepository.findByUserUsername("teacher1")).thenReturn(Optional.of(faculty));
        when(studentRepository.findById(101L)).thenReturn(Optional.of(student));
        when(attendanceRepository.findByStudentIdAndAttendanceDateAndPeriodNo(101L, date, 1))
                .thenReturn(Optional.of(existingAbsent));

        SaveAttendanceRequest request = new SaveAttendanceRequest();
        request.setYearId(1L);
        request.setDate(date);
        request.setPeriod(1);

        SaveAttendanceRequest.StudentAttendanceRequest record = new SaveAttendanceRequest.StudentAttendanceRequest();
        record.setStudentId(101L);
        record.setStatus(AttendanceRecord.AttendanceStatus.ABSENT);
        request.setRecords(List.of(record));

        teacherAttendanceService.saveAttendance("teacher1", request);

        verify(attendanceRepository, times(1)).save(any(Attendance.class));
        verify(notificationService, never()).sendAbsenceNotification(anyLong(), any(), any());
    }

    @Test
    void testSaveAttendance_BulkFiftyStudents_TenAbsent() {
        LocalDate date = LocalDate.of(2026, 8, 21);

        when(yearRepository.findById(1L)).thenReturn(Optional.of(year));
        when(academicCalendarResolver.isHoliday(any(), any())).thenReturn(false);
        when(facultyRepository.findByUserUsername("teacher1")).thenReturn(Optional.of(faculty));

        List<SaveAttendanceRequest.StudentAttendanceRequest> records = new ArrayList<>();

        for (long i = 1; i <= 50; i++) {
            Student s = Student.builder().fullName("Student " + i).regNo("REG" + i).build();
            s.setId(i);
            when(studentRepository.findById(i)).thenReturn(Optional.of(s));
            when(attendanceRepository.findByStudentIdAndAttendanceDateAndPeriodNo(i, date, 1))
                    .thenReturn(Optional.empty());

            SaveAttendanceRequest.StudentAttendanceRequest rec = new SaveAttendanceRequest.StudentAttendanceRequest();
            rec.setStudentId(i);
            // 10 absent (1..10), 40 present (11..50)
            if (i <= 10) {
                rec.setStatus(AttendanceRecord.AttendanceStatus.ABSENT);
            } else {
                rec.setStatus(AttendanceRecord.AttendanceStatus.PRESENT);
            }
            records.add(rec);
        }

        SaveAttendanceRequest request = new SaveAttendanceRequest();
        request.setYearId(1L);
        request.setDate(date);
        request.setPeriod(1);
        request.setRecords(records);

        teacherAttendanceService.saveAttendance("teacher1", request);

        verify(attendanceRepository, times(50)).save(any(Attendance.class));
        verify(notificationService, times(10)).sendAbsenceNotification(anyLong(), eq(date), eq(1));
    }
}
