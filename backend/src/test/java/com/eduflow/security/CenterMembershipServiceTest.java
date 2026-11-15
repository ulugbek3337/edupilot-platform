package com.eduflow.security;

import com.eduflow.entity.Center;
import com.eduflow.entity.CenterMembership;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.MembershipStatus;
import com.eduflow.entity.Role;
import com.eduflow.entity.User;
import com.eduflow.repository.CenterMembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CenterMembershipServiceTest {

    @Mock
    private CenterMembershipRepository membershipRepository;

    @InjectMocks
    private CenterMembershipService membershipService;

    @Test
    void activeMembershipInActiveCenterIsAccepted() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        Center center = mock(Center.class);
        when(center.getStatus()).thenReturn(CenterStatus.ACTIVE);
        CenterMembership membership = mock(CenterMembership.class);
        when(membership.getStatus()).thenReturn(MembershipStatus.ACTIVE);
        when(membership.getCenter()).thenReturn(center);
        when(membership.getRole()).thenReturn(Role.CENTER_ADMIN);
        when(membershipRepository.findByCenterIdAndUserId(12L, 7L)).thenReturn(Optional.of(membership));

        assertTrue(membershipService.isMember(user, 12L));
        assertDoesNotThrow(() -> membershipService.assertRole(user, 12L, Role.CENTER_ADMIN));
    }

    @Test
    void suspendedMembershipIsRejected() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        CenterMembership membership = mock(CenterMembership.class);
        when(membership.getStatus()).thenReturn(MembershipStatus.SUSPENDED);
        when(membershipRepository.findByCenterIdAndUserId(12L, 7L)).thenReturn(Optional.of(membership));

        assertFalse(membershipService.isMember(user, 12L));
        assertThrows(AccessDeniedException.class,
                () -> membershipService.assertRole(user, 12L, Role.CENTER_ADMIN));
    }

    @Test
    void suspendedCenterIsRejectedEvenWhenMembershipIsActive() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        Center center = mock(Center.class);
        when(center.getStatus()).thenReturn(CenterStatus.SUSPENDED);
        CenterMembership membership = mock(CenterMembership.class);
        when(membership.getStatus()).thenReturn(MembershipStatus.ACTIVE);
        when(membership.getCenter()).thenReturn(center);
        when(membershipRepository.findByCenterIdAndUserId(12L, 7L)).thenReturn(Optional.of(membership));

        assertFalse(membershipService.isMember(user, 12L));
    }
}
