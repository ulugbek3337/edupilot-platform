package com.eduflow.repository;
import com.eduflow.entity.GroupEnrollment; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface GroupEnrollmentRepository extends JpaRepository<GroupEnrollment,Long>{
 Optional<GroupEnrollment> findFirstByCenter_IdAndStudent_IdAndActiveTrue(Long centerId,Long studentId);
 boolean existsByCenter_IdAndGroup_IdAndStudent_IdAndActiveTrue(Long centerId,Long groupId,Long studentId);
 List<GroupEnrollment> findAllByCenter_IdAndGroup_IdAndActiveTrueOrderByStudent_LastNameAsc(Long centerId,Long groupId);
}
