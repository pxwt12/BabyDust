package com.babydust.api.web;

import com.babydust.api.common.ApiResponse;
import com.babydust.api.domain.BabyProfile;
import com.babydust.api.domain.MotherProfile;
import com.babydust.api.domain.PregnancyProfile;
import com.babydust.api.repository.BabyProfileRepository;
import com.babydust.api.repository.MotherProfileRepository;
import com.babydust.api.repository.PregnancyProfileRepository;
import com.babydust.api.security.CurrentUser;
import com.babydust.api.service.PermissionService;
import com.babydust.api.service.PrenatalPlanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {
    private final PregnancyProfileRepository pregnancies;
    private final MotherProfileRepository mothers;
    private final BabyProfileRepository babies;
    private final PermissionService permissions;
    private final CurrentUser currentUser;
    private final PrenatalPlanService prenatalPlans;

    public ProfileController(PregnancyProfileRepository pregnancies, MotherProfileRepository mothers, BabyProfileRepository babies, PermissionService permissions, CurrentUser currentUser, PrenatalPlanService prenatalPlans) {
        this.pregnancies = pregnancies;
        this.mothers = mothers;
        this.babies = babies;
        this.permissions = permissions;
        this.currentUser = currentUser;
        this.prenatalPlans = prenatalPlans;
    }

    @GetMapping("/mothers")
    public ApiResponse<List<MotherProfile>> listMothers(@RequestParam UUID familyId, HttpServletRequest request) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        return ApiResponse.ok(mothers.findByFamilyId(familyId));
    }

    @PostMapping("/mothers")
    public ApiResponse<MotherProfile> createMother(@Valid @RequestBody CreateMotherRequest body, HttpServletRequest request) {
        UUID userId = currentUser.id(request);
        permissions.requireFamilyMember(body.familyId(), userId);
        return ApiResponse.ok(mothers.save(new MotherProfile(body.familyId(), userId, body.birthday(), body.heightCm(), body.prePregnancyWeightKg(), body.bloodType())));
    }

    @GetMapping("/pregnancies")
    public ApiResponse<List<PregnancyProfile>> list(@RequestParam UUID familyId, HttpServletRequest request) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        return ApiResponse.ok(pregnancies.findByFamilyId(familyId));
    }

    @PostMapping("/pregnancies")
    public ApiResponse<PregnancyProfile> create(@Valid @RequestBody CreatePregnancyRequest body, HttpServletRequest request) {
        permissions.requireFamilyMember(body.familyId(), currentUser.id(request));
        LocalDate dueDate = body.dueDate() == null ? body.lmpDate().plusDays(280) : body.dueDate();
        PregnancyProfile pregnancy = pregnancies.save(new PregnancyProfile(body.familyId(), body.lmpDate(), dueDate, body.fetusCount()));
        prenatalPlans.ensureDefaultTodos(pregnancy, request.getLocale().toLanguageTag().equals("zh-CN") ? java.time.ZoneId.of("Asia/Shanghai") : java.time.ZoneId.systemDefault());
        return ApiResponse.ok(pregnancy);
    }

    public record CreatePregnancyRequest(@NotNull UUID familyId, @NotNull LocalDate lmpDate, LocalDate dueDate, @NotNull Integer fetusCount) {
    }

    @GetMapping("/babies")
    public ApiResponse<List<BabyProfile>> listBabies(@RequestParam UUID familyId, HttpServletRequest request) {
        permissions.requireFamilyMember(familyId, currentUser.id(request));
        return ApiResponse.ok(babies.findByFamilyId(familyId));
    }

    @PostMapping("/babies")
    public ApiResponse<BabyProfile> createBaby(@Valid @RequestBody CreateBabyRequest body, HttpServletRequest request) {
        permissions.requireFamilyMember(body.familyId(), currentUser.id(request));
        return ApiResponse.ok(babies.save(new BabyProfile(
                body.familyId(),
                body.pregnancyId(),
                body.name(),
                body.gender(),
                body.birthDateTime(),
                body.birthWeightKg(),
                body.birthLengthCm()
        )));
    }

    public record CreateMotherRequest(
            @NotNull UUID familyId,
            LocalDate birthday,
            @Min(80) @Max(240) Integer heightCm,
            @DecimalMin("20.0") @DecimalMax("250.0") BigDecimal prePregnancyWeightKg,
            String bloodType
    ) {
    }

    public record CreateBabyRequest(
            @NotNull UUID familyId,
            UUID pregnancyId,
            @NotBlank String name,
            @NotBlank String gender,
            OffsetDateTime birthDateTime,
            @DecimalMin("0.2") @DecimalMax("8.0") BigDecimal birthWeightKg,
            @DecimalMin("20.0") @DecimalMax("80.0") BigDecimal birthLengthCm
    ) {
    }
}
