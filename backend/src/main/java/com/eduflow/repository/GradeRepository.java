package com.eduflow.repository;
import com.eduflow.entity.GradeRecord; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface GradeRepository extends JpaRepository<GradeRecord,Long>{
 List<GradeRecord> findAllByCenter_IdAndStudent_IdOrderByCreatedAtDesc(Long centerId,Long studentId);
 Optional<GradeRecord> findByIdAndCenter_Id(Long id,Long centerId);
}
