package com.eduflow.service;

import com.eduflow.dto.StudentDtos.StudentRequest;
import com.eduflow.entity.Center;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.Student;
import com.eduflow.entity.StudentStatus;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.CenterRepository;
import com.eduflow.repository.StudentRepository;
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

    @Transactional(readOnly = true)
    public List<Student> list(Long centerId) {
        requireActiveCenter(centerId);
        return studentRepository.findAllByCenter_IdAndStatusOrderByLastNameAscFirstNameAsc(
                centerId, StudentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Student get(Long centerId, Long studentId) {
        requireActiveCenter(centerId);
        return studentRepository.findByIdAndCenter_Id(studentId, centerId)
                .filter(student -> student.getStatus() == StudentStatus.ACTIVE)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "student.not-found"));
    }

    @Transactional
    public Student create(Long centerId, StudentRequest request) {
        Center center = requireActiveCenter(centerId);
        if (studentRepository.existsByCenter_IdAndStudentNumber(centerId, request.studentNumber())) {
            throw new EduFlowException(HttpStatus.CONFLICT, "student.number.exists");
        }
        Student student = new Student(center, request.studentNumber().trim(),
                request.firstName().trim(), request.lastName().trim(),
                normalize(request.email()), normalize(request.phone()), request.dateOfBirth());
        return studentRepository.save(student);
    }

    @Transactional
    public Student update(Long centerId, Long studentId, StudentRequest request) {
        Student student = get(centerId, studentId);
        if (studentRepository.existsByCenter_IdAndStudentNumberAndIdNot(
                centerId, request.studentNumber(), studentId)) {
            throw new EduFlowException(HttpStatus.CONFLICT, "student.number.exists");
        }
        student.setStudentNumber(request.studentNumber().trim());
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
