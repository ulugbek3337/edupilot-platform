package com.eduflow.service;

import com.eduflow.dto.StudentDtos.StudentRequest;
import com.eduflow.entity.Center;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.Student;
import com.eduflow.entity.StudentStatus;
import com.eduflow.entity.User;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.CenterRepository;
import com.eduflow.repository.RefreshTokenRepository;
import com.eduflow.repository.GroupEnrollmentRepository;
import com.eduflow.repository.ParentStudentLinkRepository;
import com.eduflow.repository.StudentRepository;
import com.eduflow.security.TenantRlsContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CenterRepository centerRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private GroupEnrollmentRepository groupEnrollments;

    @Mock
    private ParentStudentLinkRepository parentLinks;

    @Mock
    private TenantRlsContext tenantRlsContext;

    @InjectMocks
    private StudentService studentService;

    @Test
    void createsStudentInsideRequestedCenter() {
        Center center = new Center("Alpha", "alpha", CenterStatus.ACTIVE);
        when(centerRepository.findById(10L)).thenReturn(Optional.of(center));
        when(studentRepository.existsByCenter_IdAndStudentNumber(10L, "S-001")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Student result = studentService.create(10L,
                new StudentRequest("S-001", " Ali ", " Valiyev ", null, " +998 90 ", LocalDate.of(2010, 1, 1)));

        assertEquals("S-001", result.getStudentNumber());
        assertEquals("Ali", result.getFirstName());
        assertEquals("Valiyev", result.getLastName());
        assertEquals("+998 90", result.getPhone());
        assertSame(center, result.getCenter());
    }

    @Test
    void rejectsDuplicateStudentNumberWithinSameCenter() {
        when(centerRepository.findById(10L))
                .thenReturn(Optional.of(new Center("Alpha", "alpha", CenterStatus.ACTIVE)));
        when(studentRepository.existsByCenter_IdAndStudentNumber(10L, "S-001")).thenReturn(true);

        assertThrows(EduFlowException.class, () -> studentService.create(10L,
                new StudentRequest("S-001", "Ali", "Valiyev", null, null, null)));
        verify(studentRepository, never()).save(any());
    }

    @Test
    void deactivatingStudentDisablesLoginAndRevokesRefreshTokens() {
        Center center = new Center("Alpha", "alpha", CenterStatus.ACTIVE);
        Student student = new Student(center, "S-001", "Ali", "Valiyev", null, null, null);
        User user = new User("student-10-1@students.edupilot.local", "hash", "Ali", "Valiyev");
        user.setId(700L);
        user.setStudentAccount(true);
        student.setUser(user);

        when(centerRepository.findById(10L)).thenReturn(Optional.of(center));
        when(studentRepository.findByIdAndCenter_Id(1L, 10L)).thenReturn(Optional.of(student));

        studentService.deactivate(10L, 1L);

        assertEquals(StudentStatus.INACTIVE, student.getStatus());
        assertFalse(user.isEnabled());
        verify(refreshTokenRepository).deleteByUserId(700L);
        verify(groupEnrollments).findAllByCenter_IdAndStudent_IdAndActiveTrue(10L, 1L);
        verify(parentLinks).findAllByCenter_IdAndStudent_IdAndActiveTrue(10L, 1L);
    }

    @Test
    void deactivatingStudentWithoutProvisionedAccountDoesNotRevokeAnotherUser() {
        Center center = new Center("Alpha", "alpha", CenterStatus.ACTIVE);
        Student student = new Student(center, "S-002", "Sara", "Aliyeva", null, null, null);
        when(centerRepository.findById(10L)).thenReturn(Optional.of(center));
        when(studentRepository.findByIdAndCenter_Id(2L, 10L)).thenReturn(Optional.of(student));

        studentService.deactivate(10L, 2L);

        assertEquals(StudentStatus.INACTIVE, student.getStatus());
        verifyNoInteractions(refreshTokenRepository);
        verify(groupEnrollments).findAllByCenter_IdAndStudent_IdAndActiveTrue(10L, 2L);
        verify(parentLinks).findAllByCenter_IdAndStudent_IdAndActiveTrue(10L, 2L);
    }

    @Test
    void refusesMissingTenantContext() {
        assertThrows(EduFlowException.class, () -> studentService.list(null));
        verifyNoInteractions(centerRepository, studentRepository);
    }

    @Test
    void refusesSuspendedCenter() {
        when(centerRepository.findById(10L))
                .thenReturn(Optional.of(new Center("Suspended", "suspended", CenterStatus.SUSPENDED)));

        assertThrows(EduFlowException.class, () -> studentService.list(10L));
        verifyNoInteractions(studentRepository);
    }

    @Test
    void cannotReadStudentFromAnotherCenter() {
        when(centerRepository.findById(10L))
                .thenReturn(Optional.of(new Center("Alpha", "alpha", CenterStatus.ACTIVE)));
        when(studentRepository.findByIdAndCenter_Id(99L, 10L)).thenReturn(Optional.empty());

        assertThrows(EduFlowException.class, () -> studentService.get(10L, 99L));
    }
}
