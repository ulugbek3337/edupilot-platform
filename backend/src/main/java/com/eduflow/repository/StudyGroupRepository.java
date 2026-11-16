package com.eduflow.repository;
import com.eduflow.entity.StudyGroup; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface StudyGroupRepository extends JpaRepository<StudyGroup,Long>{
 List<StudyGroup> findAllByCenter_IdAndActiveTrueOrderByNameAsc(Long centerId);
 Optional<StudyGroup> findByIdAndCenter_IdAndActiveTrue(Long id,Long centerId);
 boolean existsByCenter_IdAndGroupCode(Long centerId,String code);
 boolean existsByCenter_IdAndGroupCodeAndIdNot(Long centerId,String code,Long id);
 boolean existsByCenter_IdAndCourse_IdAndActiveTrue(Long centerId,Long courseId);
}