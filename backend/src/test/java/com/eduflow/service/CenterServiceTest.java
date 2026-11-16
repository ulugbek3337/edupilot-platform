package com.eduflow.service;

import com.eduflow.entity.Center;
import com.eduflow.entity.CenterMembership;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.Role;
import com.eduflow.entity.User;
import com.eduflow.entity.MembershipStatus;
import com.eduflow.repository.CenterMembershipRepository;
import com.eduflow.repository.CenterRepository;
import com.eduflow.security.TenantRlsContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class CenterServiceTest {

    @Mock
    private CenterRepository centerRepository;

    @Mock
    private CenterMembershipRepository centerMembershipRepository;

    @Mock
    private TenantRlsContext tenantRlsContext;

    @InjectMocks
    private CenterService centerService;

    @Test
    void listsOnlyActiveCentersForTheRequestedUser() {
        Center activeCenter = new Center("Active", "active", CenterStatus.ACTIVE);
        CenterMembership membership = new CenterMembership();
        membership.setCenter(activeCenter);
        when(centerMembershipRepository.findAllByUser_IdAndStatusAndCenter_Status(
                7L, MembershipStatus.ACTIVE, CenterStatus.ACTIVE
        )).thenReturn(List.of(membership));

        List<Center> result = centerService.findAllForUser(7L);

        assertEquals(List.of(activeCenter), result);
        verify(centerMembershipRepository).findAllByUser_IdAndStatusAndCenter_Status(
                7L, MembershipStatus.ACTIVE, CenterStatus.ACTIVE
        );
        verifyNoInteractions(centerRepository);
    }

    @Test
    void createsCenterInCreatorContextAndAddsInitialAdminMembership() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(7L);
        Center saved = new Center("Alpha Center", "alpha-center", CenterStatus.ACTIVE, owner);
        saved.setId(22L);
        when(centerRepository.saveAndFlush(any(Center.class))).thenReturn(saved);

        Center result = centerService.createCenter(" Alpha Center ", owner);

        assertSame(saved, result);
        verify(tenantRlsContext).applyUser(7L);
        verify(centerRepository).saveAndFlush(argThat(center ->
                "Alpha Center".equals(center.getName())
                        && "alpha-center".equals(center.getSlug())
                        && center.getCreatedBy() == owner));
        verify(tenantRlsContext).apply(22L);
        ArgumentCaptor<CenterMembership> membership = ArgumentCaptor.forClass(CenterMembership.class);
        verify(centerMembershipRepository).save(membership.capture());
        assertSame(saved, membership.getValue().getCenter());
        assertSame(owner, membership.getValue().getUser());
        assertEquals(Role.CENTER_ADMIN, membership.getValue().getRole());
    }

    @Test
    void rejectsCenterNamesThatCannotProduceAUniqueSafeSlug() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(7L);

        assertThrows(com.eduflow.exception.EduFlowException.class,
                () -> centerService.createCenter("Учебный центр", owner));

        verifyNoInteractions(centerRepository, centerMembershipRepository);
        verify(tenantRlsContext, never()).apply(22L);
    }

    @Test
    void nullUserIdReturnsNoCentersAndDoesNotQueryDatabase() {
        assertTrue(centerService.findAllForUser(null).isEmpty());
        verifyNoInteractions(centerMembershipRepository, centerRepository);
    }
}
