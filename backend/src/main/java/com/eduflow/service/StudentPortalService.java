package com.eduflow.service;

import com.eduflow.dto.StudentDtos.StudentSelfResponse;
import com.eduflow.entity.GroupEnrollment;
import com.eduflow.entity.Student;
import com.eduflow.entity.StudentStatus;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.GroupEnrollmentRepository;
import com.eduflow.repository.StudentRepository;
import com.eduflow.security.TenantRlsContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StudentPortalService {
    private final StudentRepository students;
    private final GroupEnrollmentRepository enrollments;
    private final TenantRlsContext tenantRlsContext;

    @Transactional(readOnly = true)
    public StudentSelfResponse me(Long centerId, Long userId) {
        tenantRlsContext.apply(centerId);
        Student student = students.findByUser_IdAndCenter_Id(userId, centerId)
                .filter(s -> s.getStatus() == StudentStatus.ACTIVE)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "student.not-found"));
        GroupEnrollment enrollment = enrollments.findFirstByCenter_IdAndStudent_IdAndActiveTrue(centerId, student.getId())
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "student.group.not-found"));
        return new StudentSelfResponse(student.getId(), student.getStudentNumber(),
                student.getFirstName(), student.getLastName(), enrollment.getGroup().getName(),
                enrollment.getGroup().getTeacher().getFirstName() + " " + enrollment.getGroup().getTeacher().getLastName());
    }
}
