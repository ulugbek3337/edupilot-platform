package com.eduflow.security;

import com.eduflow.entity.CenterMembership;
import com.eduflow.entity.Role;
import com.eduflow.entity.User;
import com.eduflow.repository.CenterMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CenterMembershipService {

    private final CenterMembershipRepository centerMembershipRepository;

    public boolean isMember(User user, Long centerId) {
        if (user == null || centerId == null) {
            return false;
        }
        return centerMembershipRepository.existsByCenterIdAndUserId(centerId, user.getId());
    }

    public void assertAccess(User user, Long centerId) {
        if (!isMember(user, centerId)) {
            throw new AccessDeniedException("User is not a member of this center.");
        }
    }

    public void assertRole(User user, Long centerId, Role... roles) {
        Optional<CenterMembership> membership = centerMembershipRepository.findByCenterIdAndUserId(centerId, user.getId());
        if (membership.isEmpty()) {
            throw new AccessDeniedException("User is not a member of this center.");
        }

        for (Role role : roles) {
            if (membership.get().getRole() == role) {
                return;
            }
        }

        throw new AccessDeniedException("User does not have required role for this action.");
    }
}
