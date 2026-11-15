package com.eduflow.security;

import com.eduflow.entity.CenterMembership;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.MembershipStatus;
import com.eduflow.entity.Role;
import com.eduflow.entity.User;
import com.eduflow.repository.CenterMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CenterMembershipService {

    private final CenterMembershipRepository centerMembershipRepository;

    @Transactional(readOnly = true)
    public boolean isMember(User user, Long centerId) {
        return user != null && isActiveMember(user.getId(), centerId);
    }

    @Transactional(readOnly = true)
    public boolean isActiveMember(Long userId, Long centerId) {
        return findActiveMembership(userId, centerId).isPresent();
    }

    @Transactional(readOnly = true)
    public void assertAccess(User user, Long centerId) {
        if (user == null || findActiveMembership(user.getId(), centerId).isEmpty()) {
            throw new AccessDeniedException("User is not an active member of this center.");
        }
    }

    @Transactional(readOnly = true)
    public void assertRole(User user, Long centerId, Role... roles) {
        CenterMembership membership = findActiveMembership(user == null ? null : user.getId(), centerId)
                .orElseThrow(() -> new AccessDeniedException("User is not an active member of this center."));

        for (Role role : roles) {
            if (membership.getRole() == role) {
                return;
            }
        }

        throw new AccessDeniedException("User does not have required role for this action.");
    }

    private Optional<CenterMembership> findActiveMembership(Long userId, Long centerId) {
        if (userId == null || centerId == null) {
            return Optional.empty();
        }

        return centerMembershipRepository.findByCenterIdAndUserId(centerId, userId)
                .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
                .filter(membership -> membership.getCenter() != null)
                .filter(membership -> membership.getCenter().getStatus() == CenterStatus.ACTIVE);
    }
}
