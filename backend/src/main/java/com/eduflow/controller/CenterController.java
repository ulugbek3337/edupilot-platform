package com.eduflow.controller;

import com.eduflow.dto.AuthDtos.CenterSummary;
import com.eduflow.entity.Center;
import com.eduflow.entity.User;
import com.eduflow.security.AuthenticatedUser;
import com.eduflow.service.AuthService;
import com.eduflow.service.CenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CenterController {

    private final CenterService centerService;
    private final AuthService authService;

    @GetMapping("/centers")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<CenterSummary>> listCenters(@AuthenticationPrincipal AuthenticatedUser principal) {
        List<CenterSummary> centers = centerService.findAllForUser(principal.id()).stream()
                .map(center -> new CenterSummary(center.getId(), center.getName(), center.getSlug(), center.getStatus().name()))
                .toList();
        return ResponseEntity.ok(centers);
    }

    @PostMapping("/centers")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CenterSummary> createCenter(@RequestParam String name,
                                                     @AuthenticationPrincipal AuthenticatedUser principal) {
        User user = authService.getCurrentUser(principal.getUsername());
        Center center = centerService.createCenter(name, user);
        return ResponseEntity.ok(new CenterSummary(center.getId(), center.getName(), center.getSlug(), center.getStatus().name()));
    }
}
