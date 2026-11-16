package com.eduflow.service;

import com.eduflow.dto.StudentDtos.StudentRequest;
import com.eduflow.entity.Center;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.Student;
import com.eduflow.entity.StudentStatus;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.CenterRepository;
import com.eduflow.repository.RefreshTokenRepository;
import com.eduflow.repository.StudentRepository;
import com.eduflow.security.TenantRlsContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final CenterRepository centerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TenantRlsContext tenantRlsContext;

    @Transactional(readOnly = true)
    public List<Student> list(Long centerId) {
        requireActiveCenter(centerId);
        tenantRlsContext.apply(centerId);
        return studentRepository.findAllByCenter_IdAndStatusOrderByLastNameAscFirstNameAsc(
                centerId, StudentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Student get(Long centerId, Long studentId) {
        requireActiveCenter(centerId);
        tenantRlsContext.apply(centerId);
        return studentRepository.findByIdAndCenter_Id(studentId, centerId)
                .filter(student -> student.getStatus() == StudentStatus.ACTIVE)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "student.not-found"));
    }

    @Transactional
    public Student create(Long centerId, StudentRequest request) {
        Center center = requireActiveCenter(centerId);
        tenantRlsContext.apply(centerId);
        String studentNumber = request.studentNumber().trim();
        if (studentRepository.existsByCenter_IdAndStudentNumber(centerId, studentNumber)) {
            throw new EduFlowException(HttpStatus.CONFLICT, "student.number.exists");
        }
        Student student = new Student(center, studentNumber,
                request.firstName().trim(), request.lastName().trim(),
                normalize(request.email()), normalize(request.phone()), request.dateOfBirth());
        return studentRepository.save(student);
    }

    @Transactional
    public Student update(Long centerId, Long studentId, StudentRequest request) {
        Student student = get(centerId, studentId);
        String studentNumber = request.studentNumber().trim();
        if (studentRepository.existsByCenter_IdAndStudentNumberAndIdNot(
                centerId, studentNumber, studentId)) {
            throw new EduFlowException(HttpStatus.CONFLICT, "student.number.exists");
        }
        student.setStudentNumber(studentNumber);
        student.setFirstName(request.firstName().trim());
        student.setLastName(request.lastName().trim());
        student.setEmail(normalize(request.email()));
        student.setPhone(normalize(request.phone()));
        student.setDateOfBirth(request.dateOfBirth());
        return student;
    }

    @Transactional
    public void deactivate(Long centerId, Long studentId) {
        Student student = get(centerId, studentId);
        student.setStatus(StudentStatus.INACTIVE);

        // Deactivation must also disable the provisioned login immediately.
        // JwtAuthenticationFilter reloads UserDetails on every request, so this
        // invalidates existing access tokens as well as preventing future logins.
        if (student.getUser() != null && student.getUser().isStudentAccount()) {
            student.getUser().setEnabled(false);
            refreshTokenRepository.deleteByUserId(student.getUser().getId());
        }
    }

    private Center requireActiveCenter(Long centerId) {
        if (centerId == null) {
            throw new EduFlowException(HttpStatus.BAD_REQUEST, "tenant.context.required");
        }
        Center center = centerRepository.findById(centerId)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "center.not-found"));
        if (center.getStatus() != CenterStatus.ACTIVE) {
            throw new EduFlowException(HttpStatus.FORBIDDEN, "tenant.center.inactive");
        }
        return center;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
