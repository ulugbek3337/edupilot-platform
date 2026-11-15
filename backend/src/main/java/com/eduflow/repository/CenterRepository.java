package com.eduflow.repository;

import com.eduflow.entity.Center;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CenterRepository extends JpaRepository<Center, Long> {
    Optional<Center> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
