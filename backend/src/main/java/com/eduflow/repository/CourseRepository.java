package com.eduflow.repository;
import com.eduflow.entity.Course; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface CourseRepository extends JpaRepository<Course,Long>{
 List<Course> findAllByCenter_IdAndActiveTrueOrderByNameAsc(Long centerId);
 Optional<Course> findByIdAndCenter_IdAndActiveTrue(Long id,Long centerId);
 boolean existsByCenter_IdAndCourseCode(Long centerId,String code);
 boolean existsByCenter_IdAndCourseCodeAndIdNot(Long centerId,String code,Long id);
 boolean existsByCenter_IdAndCourse_IdAndActiveTrue(Long centerId,Long courseId);
}