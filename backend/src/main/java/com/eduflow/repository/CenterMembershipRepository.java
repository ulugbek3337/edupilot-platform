package com.eduflow.repository;

import com.eduflow.entity.CenterMembership;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.MembershipStatus;
import com.eduflow.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CenterMembershipRepository extends JpaRepository<CenterMembership, Long> {
    @EntityGraph(attributePaths = "center")
    Optional<CenterMembership> findByCenterIdAndUserId(Long centerId, Long userId);
    boolean existsByCenterIdAndUserId(Long centerId, Long userId);
    boolean existsByCenterIdAndUserIdAndRole(Long centerId, Long userId, Role role);

    @EntityGraph(attributePaths = {"center", "user"})
    List<CenterMembership> findAllByCenter_IdAndRoleAndStatusAndUser_EnabledTrueOrderByUser_LastNameAsc(
            Long centerId, Role role, MembershipStatus membershipStatus
    );

    @EntityGraph(attributePaths = "center")
    List<CenterMembership> findAllByUser_IdAndStatusAndCenter_Status(
            Long userId,
            MembershipStatus membershipStatus,
            CenterStatus centerStatus
    );
}
