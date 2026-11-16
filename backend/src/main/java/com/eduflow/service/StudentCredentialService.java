package com.eduflow.service;

import com.eduflow.dto.StudentDtos.StudentCredentialResetResponse;
import com.eduflow.entity.Student;
import com.eduflow.entity.StudentStatus;
import com.eduflow.entity.User;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.RefreshTokenRepository;
import com.eduflow.repository.StudentRepository;
import com.eduflow.repository.UserRepository;
import com.eduflow.security.TenantRlsContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class StudentCredentialService {
    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StudentRepository students;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TenantRlsContext tenantRlsContext;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StudentCredentialResetResponse reset(Long centerId, Long studentId) {
        tenantRlsContext.apply(centerId);
        Student student = students.findByIdAndCenter_Id(studentId, centerId)
                .filter(s -> s.getStatus() == StudentStatus.ACTIVE)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "student.not-found"));

        User user = student.getUser();
        if (user == null || !user.isStudentAccount()) {
            throw new EduFlowException(HttpStatus.CONFLICT, "student.account.not-provisioned");
        }

        String temporaryPassword = generatePassword();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        users.save(user);
        refreshTokens.deleteByUserId(user.getId());

        return new StudentCredentialResetResponse(student.getId(), user.getEmail(), temporaryPassword, true);
    }

    private String generatePassword() {
        StringBuilder value = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            value.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return value.toString();
    }
}
