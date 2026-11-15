package com.eduflow.service;

import com.eduflow.entity.Center;
import com.eduflow.entity.CenterMembership;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.MembershipStatus;
import com.eduflow.repository.CenterMembershipRepository;
import com.eduflow.repository.CenterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CenterServiceTest {

    @Mock
    private CenterRepository centerRepository;

    @Mock
    private CenterMembershipRepository centerMembershipRepository;

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
    void nullUserIdReturnsNoCentersAndDoesNotQueryDatabase() {
        assertTrue(centerService.findAllForUser(null).isEmpty());
        verifyNoInteractions(centerMembershipRepository, centerRepository);
    }
}
