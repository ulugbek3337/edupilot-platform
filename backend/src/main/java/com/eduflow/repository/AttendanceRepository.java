package com.eduflow.repository;
import com.eduflow.entity.AttendanceRecord; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AttendanceRepository extends JpaRepository<AttendanceRecord,Long>{
 List<AttendanceRecord> findAllByCenter_IdAndLesson_IdOrderByStudent_LastNameAsc(Long centerId,Long lessonId);
 Optional<AttendanceRecord> findByIdAndCenter_Id(Long id,Long centerId);
}
