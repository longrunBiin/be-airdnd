package kr.kro.airbob.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class AuthResponseDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class LoginResponse {
        private long memberId;
        private String nickname;
    }
}
