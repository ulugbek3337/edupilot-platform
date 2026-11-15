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
import com.eduflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CenterService {

    private final CenterRepository centerRepository;
    private final CenterMembershipRepository centerMembershipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Center> findAllForUser(Long userId) {
        if (userId == null) {
            return List.of();
        }

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
        String slug = buildSlug(name);
        if (centerRepository.existsBySlug(slug)) {
            throw new EduFlowException(409, "center.slug.exists");
        }

        Center center = new Center(name, slug, CenterStatus.ACTIVE);
        Center saved = centerRepository.save(center);

        CenterMembership membership = new CenterMembership(saved, owner, Role.CENTER_ADMIN);
        centerMembershipRepository.save(membership);
        return saved;
    }

    public Center getById(Long centerId) {
        return centerRepository.findById(centerId)
                .orElseThrow(() -> new EduFlowException(404, "center.not-found"));
    }

    public String buildSlug(String name) {
        return name.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
