package com.eduflow.security;

import com.eduflow.entity.Center;
import com.eduflow.entity.CenterMembership;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.MembershipStatus;
import com.eduflow.entity.Role;
import com.eduflow.entity.User;
import com.eduflow.entity.UserStatus;
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
        when(user.isEnabled()).thenReturn(true);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        Center center = mock(Center.class);
        when(center.getStatus()).thenReturn(CenterStatus.ACTIVE);
        CenterMembership membership = mock(CenterMembership.class);
        when(membership.getStatus()).thenReturn(MembershipStatus.ACTIVE);
        when(membership.getUser()).thenReturn(user);
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
        when(user.isEnabled()).thenReturn(true);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        Center center = mock(Center.class);
        when(center.getStatus()).thenReturn(CenterStatus.SUSPENDED);
        CenterMembership membership = mock(CenterMembership.class);
        when(membership.getStatus()).thenReturn(MembershipStatus.ACTIVE);
        when(membership.getUser()).thenReturn(user);
        when(membership.getCenter()).thenReturn(center);
        when(membershipRepository.findByCenterIdAndUserId(12L, 7L)).thenReturn(Optional.of(membership));

        assertFalse(membershipService.isMember(user, 12L));
    }

    @Test
    void roleMustMatchMembershipInTheRequestedCenter() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.isEnabled()).thenReturn(true);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        Center center = mock(Center.class);
        when(center.getStatus()).thenReturn(CenterStatus.ACTIVE);
        CenterMembership membership = mock(CenterMembership.class);
        when(membership.getStatus()).thenReturn(MembershipStatus.ACTIVE);
        when(membership.getUser()).thenReturn(user);
        when(membership.getCenter()).thenReturn(center);
        when(membership.getRole()).thenReturn(Role.TEACHER);
        when(membershipRepository.findByCenterIdAndUserId(12L, 7L)).thenReturn(Optional.of(membership));

        assertDoesNotThrow(() -> membershipService.assertRole(user, 12L, Role.TEACHER));
        assertThrows(AccessDeniedException.class,
                () -> membershipService.assertRole(user, 12L, Role.CENTER_ADMIN));
    }

    @Test
    void lockedUserIsRejectedEvenWhenMembershipAndCenterAreActive() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.isEnabled()).thenReturn(true);
        when(user.getStatus()).thenReturn(UserStatus.LOCKED);
        CenterMembership membership = mock(CenterMembership.class);
        when(membership.getStatus()).thenReturn(MembershipStatus.ACTIVE);
        when(membership.getUser()).thenReturn(user);
        when(membershipRepository.findByCenterIdAndUserId(12L, 7L)).thenReturn(Optional.of(membership));

        assertFalse(membershipService.isActiveMember(7L, 12L));
        assertThrows(AccessDeniedException.class,
                () -> membershipService.assertRole(user, 12L, Role.CENTER_ADMIN));
    }

    @Test
    void membershipInAnotherCenterDoesNotGrantAccess() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(membershipRepository.findByCenterIdAndUserId(99L, 7L)).thenReturn(Optional.empty());

        assertFalse(membershipService.isActiveMember(7L, 99L));
        assertThrows(AccessDeniedException.class,
                () -> membershipService.assertRole(user, 99L, Role.CENTER_ADMIN));
        verify(membershipRepository, times(2)).findByCenterIdAndUserId(99L, 7L);
    }

    @Test
    void nullUserOrCenterNeverGrantsAccess() {
        assertFalse(membershipService.isMember(null, 12L));
        assertFalse(membershipService.isActiveMember(7L, null));
        assertThrows(AccessDeniedException.class,
                () -> membershipService.assertRole(null, 12L, Role.CENTER_ADMIN));
        verifyNoInteractions(membershipRepository);
    }
}