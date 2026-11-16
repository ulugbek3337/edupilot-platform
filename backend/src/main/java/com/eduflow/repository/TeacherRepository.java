package com.eduflow.repository;
import com.eduflow.entity.Teacher; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface TeacherRepository extends JpaRepository<Teacher,Long>{
 List<Teacher> findAllByCenter_IdAndStatusOrderByLastNameAscFirstNameAsc(Long centerId,String status);
 Optional<Teacher> findByIdAndCenter_Id(Long id,Long centerId);
 boolean existsByCenter_IdAndTeacherCode(Long centerId,String code);
 boolean existsByCenter_IdAndTeacherCodeAndIdNot(Long centerId,String code,Long id);
}