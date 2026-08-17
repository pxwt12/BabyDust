package com.babydust.api.web;

import com.babydust.api.common.ApiResponse;
import com.babydust.api.domain.AiAuditLog;
import com.babydust.api.domain.AiDraftConfirmation;
import com.babydust.api.repository.AiAuditLogRepository;
import com.babydust.api.repository.AiDraftConfirmationRepository;
import com.babydust.api.security.CurrentUser;
import com.babydust.api.service.AiDraftConfirmationService;
import com.babydust.api.service.AiGatewayService;
import com.babydust.api.service.AiRateLimiter;
import com.babydust.api.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiGatewayService aiGateway;
    private final AiDraftConfirmationService draftConfirmation;
    private final AiAuditLogRepository auditLogs;
    private final AiDraftConfirmationRepository draftConfirmations;
    private final PermissionService permissions;
    private final CurrentUser currentUser;
    private final AiRateLimiter rateLimiter;

    public AiController(AiGatewayService aiGateway, AiDraftConfirmationService draftConfirmation, AiAuditLogRepository auditLogs, AiDraftConfirmationRepository draftConfirmations, PermissionService permissions, CurrentUser currentUser, AiRateLimiter rateLimiter) {
        this.aiGateway = aiGateway;
        this.draftConfirmation = draftConfirmation;
        this.auditLogs = auditLogs;
        this.draftConfirmations = draftConfirmations;
        this.permissions = permissions;
        this.currentUser = currentUser;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/extract-record")
    public ApiResponse<AiGatewayService.AiDraftResponse> extractRecord(@Valid @RequestBody ExtractTextRequest request, HttpServletRequest httpRequest) {
        rateLimiter.check(rateLimitSubject(httpRequest), "extract_record");
        return ApiResponse.ok(aiGateway.extractRecord(request.text(), request.inputType()));
    }

    @PostMapping("/extract-report")
    public ApiResponse<AiGatewayService.AiDraftResponse> extractReport(@Valid @RequestBody ExtractTextRequest request, HttpServletRequest httpRequest) {
        rateLimiter.check(rateLimitSubject(httpRequest), "extract_report");
        return ApiResponse.ok(aiGateway.extractReport(request.text(), request.inputType()));
    }

    @PostMapping("/ocr-report")
    public ApiResponse<AiGatewayService.AiPreprocessResponse> ocrReport(@Valid @RequestBody PreprocessRequest request, HttpServletRequest httpRequest) {
        rateLimiter.check(rateLimitSubject(httpRequest), "ocr_report");
        return ApiResponse.ok(aiGateway.ocrReport(request.fileUrl(), request.text()));
    }

    @PostMapping("/asr-record")
    public ApiResponse<AiGatewayService.AiPreprocessResponse> asrRecord(@Valid @RequestBody PreprocessRequest request, HttpServletRequest httpRequest) {
        rateLimiter.check(rateLimitSubject(httpRequest), "asr_record");
        return ApiResponse.ok(aiGateway.asrRecord(request.fileUrl(), request.text()));
    }

    @PostMapping("/qa")
    public ApiResponse<AiGatewayService.AiQaResponse> qa(@Valid @RequestBody QaRequest request, HttpServletRequest httpRequest) {
        rateLimiter.check(rateLimitSubject(httpRequest), "qa");
        return ApiResponse.ok(aiGateway.answerQuestion(request.question(), request.locale()));
    }

    @PostMapping("/confirm-draft")
    public ApiResponse<AiDraftConfirmationService.AiDraftConfirmationResult> confirmDraft(@Valid @RequestBody ConfirmDraftRequest request, HttpServletRequest httpRequest) {
        UUID userId = currentUser.id(httpRequest);
        permissions.requireFamilyMember(request.familyId(), userId);
        return ApiResponse.ok(draftConfirmation.confirm(new AiDraftConfirmationService.AiDraftConfirmationRequest(
                request.familyId(),
                userId,
                request.subjectType(),
                request.subjectId(),
                request.draft()
        )));
    }

    @org.springframework.web.bind.annotation.GetMapping("/draft-confirmations")
    public ApiResponse<List<AiDraftConfirmation>> draftConfirmations(@RequestParam UUID familyId, HttpServletRequest httpRequest) {
        permissions.requireFamilyMember(familyId, currentUser.id(httpRequest));
        return ApiResponse.ok(draftConfirmations.findTop50ByFamilyIdOrderByConfirmedAtDesc(familyId));
    }

    @org.springframework.web.bind.annotation.GetMapping("/audit-logs")
    public ApiResponse<java.util.List<AiAuditLog>> auditLogs() {
        return ApiResponse.ok(auditLogs.findTop20ByOrderByCreatedAtDesc());
    }

    private String rateLimitSubject(HttpServletRequest request) {
        String userHeader = request.getHeader("X-Dev-User-Id");
        if (userHeader != null && !userHeader.isBlank()) {
            return "user:" + userHeader;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    public record ExtractTextRequest(@NotBlank String text, String inputType) {
    }

    public record PreprocessRequest(@NotBlank String fileUrl, String text) {
    }

    public record QaRequest(@NotBlank String question, String locale) {
    }

    public record ConfirmDraftRequest(@NotNull UUID familyId, @NotBlank String subjectType, @NotNull UUID subjectId, @NotBlank String draft) {
    }
}
