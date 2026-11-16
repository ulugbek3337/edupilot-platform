package com.eduflow.service;

import com.eduflow.entity.Center;
import com.eduflow.entity.CenterMembership;
import com.eduflow.entity.CenterStatus;
import com.eduflow.entity.MembershipStatus;
import com.eduflow.entity.Role;
import com.eduflow.entity.User;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.CenterMembershipRepository;
import com.eduflow.repository.CenterRepository;
import com.eduflow.security.TenantRlsContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CenterService {

    private final CenterRepository centerRepository;
    private final CenterMembershipRepository centerMembershipRepository;
    private final TenantRlsContext tenantRlsContext;

    @Transactional(readOnly = true)
    public List<Center> findAllForUser(Long userId) {
        if (userId == null) {
            return List.of();
        }

        tenantRlsContext.applyUser(userId);
        return centerMembershipRepository.findAllByUser_IdAndStatusAndCenter_Status(
                        userId,
                        MembershipStatus.ACTIVE,
                        CenterStatus.ACTIVE
                ).stream()
                .map(CenterMembership::getCenter)
                .toList();
    }

    @Transactional
    public Center createCenter(String name, User owner) {
        if (owner == null || owner.getId() == null) {
            throw new EduFlowException(HttpStatus.UNAUTHORIZED, "auth.invalid-credentials");
        }
        if (name == null || name.isBlank() || name.trim().length() > 150) {
            throw new EduFlowException(HttpStatus.BAD_REQUEST, "center.name.invalid");
        }

        String normalizedName = name.trim();
        String slug = buildSlug(normalizedName);
        if (slug.isBlank()) {
            throw new EduFlowException(HttpStatus.BAD_REQUEST, "center.name.invalid");
        }

        // The creator identity is required by the centers INSERT RLS policy.
        tenantRlsContext.applyUser(owner.getId());
        Center center = new Center(normalizedName, slug, CenterStatus.ACTIVE, owner);
        final Center saved;
        try {
            // The database unique constraint is authoritative across tenants.
            // A pre-check would be hidden by center RLS and could leak slugs.
            saved = centerRepository.saveAndFlush(center);
        } catch (DataIntegrityViolationException exception) {
            throw new EduFlowException(HttpStatus.CONFLICT, "center.slug.exists");
        }

        tenantRlsContext.apply(saved.getId());
        centerMembershipRepository.save(new CenterMembership(saved, owner, Role.CENTER_ADMIN));
        return saved;
    }

    @Transactional(readOnly = true)
    public Center getById(Long centerId) {
        if (centerId == null) {
            throw new EduFlowException(HttpStatus.BAD_REQUEST, "tenant.context.required");
        }
        tenantRlsContext.apply(centerId);
        return centerRepository.findById(centerId)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "center.not-found"));
    }

    public String buildSlug(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
