package com.eduflow.service;

import com.eduflow.dto.StudentDtos.StudentRequest;
import com.eduflow.entity.Center;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.Student;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.CenterRepository;
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
