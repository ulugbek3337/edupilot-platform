package com.eduflow.repository;
import com.eduflow.entity.Lesson; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface LessonRepository extends JpaRepository<Lesson,Long>{
 List<Lesson> findAllByCenter_IdAndGroup_IdOrderByStartsAtAsc(Long centerId,Long groupId);
 Optional<Lesson> findByIdAndCenter_Id(Long id,Long centerId);
}
