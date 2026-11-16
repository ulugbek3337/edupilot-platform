package com.eduflow.service;

import com.eduflow.dto.PortalDtos.*;
import com.eduflow.entity.*;
import com.eduflow.exception.EduFlowException;
import com.eduflow.repository.*;
import com.eduflow.security.TenantRlsContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParentPortalService {
    private final CenterRepository centers;
    private final StudentRepository students;
    private final ParentStudentLinkRepository links;
    private final CenterMembershipRepository memberships;
    private final GradeRepository grades;
    private final AttendanceRepository attendance;
    private final TenantRlsContext tenantRls;
    private final AuditLogService audit;

    private void apply(Long centerId) {
        if (centerId == null) {
            throw new EduFlowException(HttpStatus.BAD_REQUEST, "tenant.context.required");
        }
        Center center = centers.findById(centerId)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "center.not-found"));
        if (center.getStatus() != CenterStatus.ACTIVE) {
            throw new EduFlowException(HttpStatus.FORBIDDEN, "tenant.center.inactive");
        }
        tenantRls.apply(centerId);
    }

    @Transactional
    public ParentStudentLink link(Long centerId, Long parentId, Long studentId, Long actorId) {
        apply(centerId);
        CenterMembership parentMembership = memberships.findByCenterIdAndUserId(centerId, parentId)
                .filter(membership -> membership.getRole() == Role.PARENT
                        && membership.getStatus() == MembershipStatus.ACTIVE
                        && membership.getUser().isEnabled()
                        && membership.getUser().getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "parent.not-found"));
        User parent = parentMembership.getUser();

        Student student = students.findByIdAndCenter_Id(studentId, centerId)
                .filter(value -> value.getStatus() == StudentStatus.ACTIVE)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "student.not-found"));

        Optional<ParentStudentLink> existing =
                links.findByCenter_IdAndParent_IdAndStudent_Id(centerId, parentId, studentId);
        if (existing.isPresent()) {
            ParentStudentLink link = existing.get();
            if (link.isActive()) {
                throw new EduFlowException(HttpStatus.CONFLICT, "parent.link.exists");
            }
            link.setActive(true);
            link = links.save(link);
            audit.record(centerId, actorId, "PARENT_LINK_REACTIVATED", "ParentStudentLink",
                    link.getId(), "Student " + studentId + " link restored for parent " + parentId);
            return link;
        }

        ParentStudentLink link = links.save(new ParentStudentLink(student.getCenter(), parent, student));
        audit.record(centerId, actorId, "PARENT_LINK_CREATED", "ParentStudentLink",
                link.getId(), "Student " + studentId + " linked to parent " + parentId);
        return link;
    }

    @Transactional(readOnly = true)
    public List<ParentAccountOption> listParentAccounts(Long centerId) {
        apply(centerId);
        return memberships.findAllByCenter_IdAndRoleAndStatusAndUser_EnabledTrueAndUser_StatusOrderByUser_LastNameAsc(
                        centerId, Role.PARENT, MembershipStatus.ACTIVE, UserStatus.ACTIVE)
                .stream()
                .map(membership -> new ParentAccountOption(membership.getUser().getId(),
                        membership.getUser().getEmail(), membership.getUser().getFirstName(),
                        membership.getUser().getLastName()))
                .toList();
    }

    @Transactional
    public void unlink(Long centerId, Long linkId, Long actorId) {
        apply(centerId);
        ParentStudentLink link = links.findById(linkId)
                .filter(value -> value.getCenter().getId().equals(centerId) && value.isActive())
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "parent.link.not-found"));
        link.setActive(false);
        audit.record(centerId, actorId, "PARENT_LINK_REVOKED", "ParentStudentLink",
                link.getId(), "Parent link revoked for student " + link.getStudent().getId());
    }

    @Transactional(readOnly = true)
    public List<ParentLinkResponse> listLinks(Long centerId) {
        apply(centerId);
        return links.findAllByCenter_IdAndActiveTrueOrderByCreatedAtDesc(centerId).stream()
                .map(link -> new ParentLinkResponse(link.getId(), link.getParent().getId(),
                        link.getParent().getEmail(), link.getStudent().getId(),
                        link.getStudent().getFirstName() + " " + link.getStudent().getLastName(),
                        link.isActive(), link.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChildResponse> children(Long centerId, Long parentId) {
        apply(centerId);
        return links.findAllByCenter_IdAndParent_IdAndActiveTrueOrderByStudent_LastNameAsc(centerId, parentId)
                .stream()
                .filter(link -> link.getStudent().getStatus() == StudentStatus.ACTIVE)
                .map(link -> new ChildResponse(link.getStudent().getId(), link.getStudent().getStudentNumber(),
                        link.getStudent().getFirstName(), link.getStudent().getLastName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParentGradeResponse> grades(Long centerId, Long parentId, Long studentId) {
        apply(centerId);
        requireLink(centerId, parentId, studentId);
        return grades.findAllByCenter_IdAndStudent_IdOrderByCreatedAtDesc(centerId, studentId).stream()
                .map(grade -> new ParentGradeResponse(grade.getId(), grade.getTitle(), grade.getScore(),
                        grade.getMaxScore(), grade.getComment(), grade.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ParentAttendanceResponse> attendance(Long centerId, Long parentId, Long studentId) {
        apply(centerId);
        requireLink(centerId, parentId, studentId);
        return attendance.findAllByCenter_IdAndStudent_IdOrderByCreatedAtDesc(centerId, studentId).stream()
                .map(record -> new ParentAttendanceResponse(record.getId(), record.getLesson().getId(),
                        record.getLesson().getTitle(), record.getStatus(), record.getNote(), record.getCreatedAt()))
                .toList();
    }

    private void requireLink(Long centerId, Long parentId, Long studentId) {
        ParentStudentLink link = links.findByCenter_IdAndParent_IdAndStudent_IdAndActiveTrue(
                        centerId, parentId, studentId)
                .orElseThrow(() -> new EduFlowException(HttpStatus.NOT_FOUND, "parent.child.not-found"));
        if (link.getStudent().getStatus() != StudentStatus.ACTIVE) {
            throw new EduFlowException(HttpStatus.NOT_FOUND, "parent.child.not-found");
        }
    }
}
