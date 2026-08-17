package com.babydust.api.web;

import com.babydust.api.common.ApiResponse;
import com.babydust.api.domain.AppUser;
import com.babydust.api.domain.SessionToken;
import com.babydust.api.repository.AppUserRepository;
import com.babydust.api.repository.SessionTokenRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AppUserRepository users;
    private final SessionTokenRepository tokens;

    public AuthController(AppUserRepository users, SessionTokenRepository tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    @PostMapping("/wechat-login")
    public ApiResponse<Map<String, Object>> wechatLogin(@Valid @RequestBody WechatLoginRequest request) {
        String pseudoOpenid = "dev_" + UUID.nameUUIDFromBytes(request.code().getBytes(StandardCharsets.UTF_8));
        AppUser user = users.findByOpenid(pseudoOpenid)
                .orElseGet(() -> users.save(new AppUser(pseudoOpenid, request.nickname())));
        SessionToken session = tokens.save(new SessionToken("bd_" + UUID.randomUUID(), user.getId(), OffsetDateTime.now().plusDays(30)));
        return ApiResponse.ok(Map.of("token", session.getToken(), "userId", user.getId(), "nickname", user.getNickname(), "expiresAt", session.getExpiresAt()));
    }

    public record WechatLoginRequest(@NotBlank String code, String nickname) {
        public WechatLoginRequest {
            if (nickname == null || nickname.isBlank()) {
                nickname = "Jie Hao Yun User";
            }
        }
    }
}
