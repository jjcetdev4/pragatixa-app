package jjcet.PragatiX;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import jjcet.PragatiX.entity.*;
import jjcet.PragatiX.repository.*;
import jjcet.PragatiX.modules.authentication.repository.*;
import jjcet.PragatiX.modules.student.repository.StudentRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@SpringBootTest
public class SeedCredentialsDatabaseTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SubRoleRepository subRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private GenderRepository genderRepository;

    @Autowired
    private AcademicYearRepository academicYearRepository;

    @Autowired
    private YearRepository yearRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Test
    @Transactional
    @Rollback(false)
    public void seedCredentialsIntoDatabase() {
        System.out.println("=================================================================");
        System.out.println("SEEDING TARGET CREDENTIALS INTO pragatix_v1 DATABASE...");
        System.out.println("=================================================================");

        Department dept = getOrCreateDepartment();
        Section sec = getOrCreateSection(dept);
        Gender gen = getOrCreateGender();
        AcademicYear ay = getOrCreateAcademicYear();
        Year yr = getOrCreateYear();
        Semester sem = getOrCreateSemester();

        Role superAdminRole = getOrCreateRole("ROLE_SUPER_ADMIN");
        Role adminRole = getOrCreateRole("ROLE_ADMIN");
        Role teacherRole = getOrCreateRole("ROLE_TEACHER");
        Role hodRole = getOrCreateRole("ROLE_HOD");
        Role studentRole = getOrCreateRole("ROLE_STUDENT");

        SubRole ccSubRole = getOrCreateSubRole("CC", teacherRole);
        SubRole hodSubRole = getOrCreateSubRole("HOD", teacherRole);

        String hashedPass = passwordEncoder.encode("1234");

        // 1. Super Admin: superadmin@gmail.com
        seedUser("superadmin@gmail.com", "superadmin", "Super Admin", hashedPass,
                Set.of(superAdminRole), Set.of(), dept, sec, "1st Year", jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

        // 2. Admin: admin@gmail.com
        seedUser("admin@gmail.com", "admin", "Admin", hashedPass,
                Set.of(adminRole), Set.of(), dept, sec, "First Year", jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

        // 3. HOD: hod@gmail.com
        seedUser("hod@gmail.com", "hod", "HOD", hashedPass,
                Set.of(teacherRole, hodRole), Set.of(hodSubRole), dept, sec, "1st Year", jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

        // 4. CC: cc@gmail.com
        seedUser("cc@gmail.com", "cc", "Class Coordinator", hashedPass,
                Set.of(teacherRole), Set.of(ccSubRole), dept, sec, "1st Year", jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

        // 5. Teacher: teacher@gmail.com
        seedUser("teacher@gmail.com", "teacher", "Teacher", hashedPass,
                Set.of(teacherRole), Set.of(), dept, sec, "1st Year", jjcet.PragatiX.enums.AcademicYear.FIRST_YEAR);

        // 6. Student: student@gmail.com
        seedStudent("student@gmail.com", "STD2026001", "SPR2026001", "Student", hashedPass,
                dept, sec, gen, ay, yr, sem);

        System.out.println("=================================================================");
        System.out.println("SUCCESSFULLY PERSISTED ALL 6 CREDENTIALS INTO DATABASE!");
        System.out.println("=================================================================");
    }

    private void seedUser(String email, String username, String fullName, String password, Set<Role> roles,
            Set<SubRole> subRoles, Department dept, Section sec, String year, jjcet.PragatiX.enums.AcademicYear academicYear) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = userRepository.findByUsername(username).orElse(null);
        }
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setUsername(username);
        }
        user.setFullName(fullName);
        user.setRoles(roles != null ? new java.util.HashSet<>(roles) : new java.util.HashSet<>());
        user.setSubRoles(subRoles != null ? new java.util.HashSet<>(subRoles) : new java.util.HashSet<>());
        user.setDepartment(dept);
        user.setSection(sec);
        user.setYear(year);
        user.setAcademicYear(academicYear);
        user.setActive(true);
        user.setDeleted(false);
        userRepository.save(user);
        System.out.println("Saved User: " + email + " (" + username + ") with roles=" + roles + ", subroles=" + subRoles);
    }

    private void seedStudent(String email, String regNo, String sprNo, String fullName, String password,
            Department dept, Section sec, Gender gen, AcademicYear ay, Year yr, Semester sem) {
        Student s = studentRepository.findByEmail(email).orElse(null);
        if (s == null) {
            s = studentRepository.findByRegNo(regNo).orElse(null);
        }
        if (s == null) {
            s = new Student();
            s.setEmail(email);
            s.setRegNo(regNo);
            s.setSprNo(sprNo);
        }
        s.setFullName(fullName);
        s.setDepartment(dept);
        s.setSection(sec);
        s.setGenderRef(gen);
        s.setYearRef(yr);
        s.setSemesterRef(sem);
        s.setPhoneNo("1234567890");
        s.setCaptain(false);
        s.setActive(true);
        s.setDeleted(false);
        if (s.getTotalXp() == 0) {
            s.setScore(0);
            s.setTotalXp(0);
            s.setGroupXp(0);
            s.setIndividualXp(0);
            s.setMustXp(0);
            s.setStage(1);
            s.setCurrentStage(1);
        }
        studentRepository.save(s);
        System.out.println("Saved Student: " + email + " (" + regNo + ")");
    }

    private Department getOrCreateDepartment() {
        List<Department> list = departmentRepository.findAll();
        for (Department d : list) {
            if ("Information Technology".equalsIgnoreCase(d.getName()) || "IT".equalsIgnoreCase(d.getDeptCode())
                    || "Computer Science and Engineering".equalsIgnoreCase(d.getName()) || "CSE".equalsIgnoreCase(d.getDeptCode())) {
                return d;
            }
        }
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Department d = new Department();
        d.setDeptCode("CSE");
        d.setDeptName("Computer Science and Engineering");
        d.setName("Computer Science and Engineering");
        return departmentRepository.save(d);
    }

    private Section getOrCreateSection(Department dept) {
        if (dept != null && dept.getId() != null) {
            List<Section> list = sectionRepository.findByDepartment_Id(dept.getId());
            if (!list.isEmpty()) {
                return list.get(0);
            }
        }
        List<Section> list = sectionRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Section s = new Section();
        s.setSectionName("A");
        s.setDepartment(dept);
        return sectionRepository.save(s);
    }

    private Gender getOrCreateGender() {
        List<Gender> list = genderRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Gender g = new Gender();
        g.setGenderName("Male");
        return genderRepository.save(g);
    }

    private AcademicYear getOrCreateAcademicYear() {
        List<AcademicYear> list = academicYearRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        AcademicYear ay = new AcademicYear();
        ay.setAcademicYear("2026-2027");
        ay.setStartDate(LocalDate.now());
        ay.setEndDate(LocalDate.now().plusYears(1));
        ay.setStatus(AcademicYear.Status.ACTIVE);
        return academicYearRepository.save(ay);
    }

    private Year getOrCreateYear() {
        List<Year> list = yearRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Year y = new Year();
        y.setYearNo((byte) 1);
        y.setYearName("1st Year");
        return yearRepository.save(y);
    }

    private Semester getOrCreateSemester() {
        List<Semester> list = semesterRepository.findAll();
        if (!list.isEmpty()) {
            return list.get(0);
        }
        Semester sem = new Semester();
        sem.setSemesterNo((byte) 1);
        sem.setSemesterName("Semester 1");
        return semesterRepository.save(sem);
    }

    private Role getOrCreateRole(String name) {
        Optional<Role> r = roleRepository.findByName(name);
        if (r.isPresent())
            return r.get();

        String rawName = name.replace("ROLE_", "");
        Optional<Role> rRaw = roleRepository.findByName(rawName);
        if (rRaw.isPresent())
            return rRaw.get();

        Role newRole = new Role();
        newRole.setName(name);
        return roleRepository.save(newRole);
    }

    private SubRole getOrCreateSubRole(String name, Role parentRole) {
        return subRoleRepository.findByName(name)
                .orElseGet(() -> {
                    SubRole sr = new SubRole();
                    sr.setName(name);
                    sr.setRole(parentRole);
                    return subRoleRepository.save(sr);
                });
    }
}
