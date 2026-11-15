package com.eduflow.security;

import com.eduflow.entity.Center;
import com.eduflow.entity.CenterMembership;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.MembershipStatus;
import com.eduflow.entity.Role;
import com.eduflow.entity.User;
import com.eduflow.entity.UserStatus;
import com.eduflow.repository.CenterMembershipRepository;
import com.eduflow.repository.CenterRepository;
import com.eduflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "DB_URL=jdbc:h2:mem:edupilot_it;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "DB_USERNAME=sa",
        "DB_PASSWORD=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "JWT_EXPIRATION_MS=3600000",
        "JWT_REFRESH_EXPIRATION_MS=604800000"
})
class TenantIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CenterRepository centerRepository;

    @Autowired
    private CenterMembershipRepository membershipRepository;

    @Test
    void centersEndpointRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/centers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void authenticatedUserSeesOnlyCentersWithActiveMembershipAndActiveCenter() throws Exception {
        User user = activeUser("teacher@example.test");
        User otherUser = activeUser("other@example.test");

        Center allowedCenter = centerRepository.save(new Center("Allowed Center", "allowed-center", CenterStatus.ACTIVE));
        Center suspendedCenter = centerRepository.save(new Center("Suspended Center", "suspended-center", CenterStatus.SUSPENDED));
        Center otherTenant = centerRepository.save(new Center("Other Tenant", "other-tenant", CenterStatus.ACTIVE));
        Center inactiveMembershipCenter = centerRepository.save(
                new Center("Inactive Membership", "inactive-membership", CenterStatus.ACTIVE));

        membershipRepository.save(new CenterMembership(allowedCenter, user, Role.TEACHER));

        CenterMembership suspendedCenterMembership = new CenterMembership(suspendedCenter, user, Role.TEACHER);
        membershipRepository.save(suspendedCenterMembership);

        CenterMembership inactiveMembership = new CenterMembership(inactiveMembershipCenter, user, Role.TEACHER);
        inactiveMembership.setStatus(MembershipStatus.SUSPENDED);
        membershipRepository.save(inactiveMembership);

        membershipRepository.save(new CenterMembership(otherTenant, otherUser, Role.CENTER_ADMIN));

        AuthenticatedUser principal = AuthenticatedUser.from(user);
        UsernamePasswordAuthenticationToken auth = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/v1/centers")
                        .with(authentication(auth))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(allowedCenter.getId()))
                .andExpect(jsonPath("$[0].slug").value("allowed-center"));
    }

    private User activeUser(String email) {
        User user = new User(email, "not-a-real-password-hash", "Integration", "Test");
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }
}
