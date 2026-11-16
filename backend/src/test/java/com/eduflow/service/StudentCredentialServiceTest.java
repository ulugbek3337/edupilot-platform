package com.eduflow.service;

import com.eduflow.entity.Center;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.Student;
import com.eduflow.entity.User;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.RefreshTokenRepository;
import com.eduflow.repository.StudentRepository;
import com.eduflow.repository.UserRepository;
import com.eduflow.security.TenantRlsContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentCredentialServiceTest {
    @Mock private StudentRepository students;
    @Mock private UserRepository users;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private TenantRlsContext tenantRlsContext;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private StudentCredentialService service;

    @Test
    void resetsOnlyOwnCenterStudentAndRequiresPasswordChange() {
        Center center = new Center("Alpha", "alpha", CenterStatus.ACTIVE);
        Student student = new Student(center, "S-001", "Ali", "Valiyev", null, null, null);
        student.setId(5L);
        User user = new User("student-1-5@students.edupilot.local", "old-hash", "Ali", "Valiyev");
        user.setId(9L);
        user.setStudentAccount(true);
        student.setUser(user);
        when(students.findByIdAndCenter_Id(5L, 1L)).thenReturn(Optional.of(student));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");

        var result = service.reset(1L, 5L);

        assertEquals(5L, result.studentId());
        assertEquals(user.getEmail(), result.login());
        assertEquals(16, result.temporaryPassword().length());
        assertTrue(result.mustChangePassword());
        assertEquals("new-hash", user.getPasswordHash());
        assertTrue(user.isMustChangePassword());
        verify(tenantRlsContext).apply(1L);
        verify(users).save(user);
        verify(refreshTokens).deleteByUserId(user.getId());
    }

    @Test
    void doesNotResetStudentOutsideCurrentCenter() {
        when(students.findByIdAndCenter_Id(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(EduFlowException.class, () -> service.reset(1L, 5L));

        verifyNoInteractions(users, refreshTokens, passwordEncoder);
    }

    @Test
    void refusesLegacyStudentWithoutProvisionedLogin() {
        Center center = new Center("Alpha", "alpha", CenterStatus.ACTIVE);
        Student student = new Student(center, "S-001", "Ali", "Valiyev", null, null, null);
        when(students.findByIdAndCenter_Id(5L, 1L)).thenReturn(Optional.of(student));

        assertThrows(EduFlowException.class, () -> service.reset(1L, 5L));

        verifyNoInteractions(users, refreshTokens, passwordEncoder);
    }
}
