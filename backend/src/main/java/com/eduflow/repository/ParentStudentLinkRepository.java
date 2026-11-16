package com.eduflow.repository;
import com.eduflow.entity.ParentStudentLink; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface ParentStudentLinkRepository extends JpaRepository<ParentStudentLink,Long>{
 List<ParentStudentLink> findAllByCenter_IdAndParent_IdAndActiveTrueOrderByStudent_LastNameAsc(Long centerId,Long parentId);
 List<ParentStudentLink> findAllByCenter_IdAndActiveTrueOrderByCreatedAtDesc(Long centerId);
 Optional<ParentStudentLink> findByCenter_IdAndParent_IdAndStudent_IdAndActiveTrue(Long centerId,Long parentId,Long studentId);
 boolean existsByCenter_IdAndParent_IdAndStudent_IdAndActiveTrue(Long centerId,Long parentId,Long studentId);
 List<ParentStudentLink> findAllByCenter_IdAndStudent_IdAndActiveTrue(Long centerId,Long studentId);
}
