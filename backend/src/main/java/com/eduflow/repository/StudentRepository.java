package com.eduflow.repository;

import com.eduflow.entity.Student;
import com.eduflow.entity.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findAllByCenter_IdAndStatusOrderByLastNameAscFirstNameAsc(
            Long centerId, StudentStatus status);

    Optional<Student> findByUser_IdAndCenter_Id(Long userId, Long centerId);
    Optional<Student> findByIdAndCenter_Id(Long id, Long centerId);

    boolean existsByCenter_IdAndStudentNumber(Long centerId, String studentNumber);

    boolean existsByCenter_IdAndStudentNumberAndIdNot(
            Long centerId, String studentNumber, Long id);
}
