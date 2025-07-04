package kr.kro.airbob.domain.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import kr.kro.airbob.domain.auth.dto.AuthRequestDto.LoginRequest;
import kr.kro.airbob.domain.auth.dto.AuthResponseDto;
import kr.kro.airbob.domain.auth.dto.AuthResponseDto.LoginResponse;
import kr.kro.airbob.domain.auth.dto.MemberSessionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponseDto.LoginResponse login(@RequestBody LoginRequest request, HttpServletResponse response) {
        MemberSessionDto memberSessionDto = authService.login(request.getEmail(), request.getPassword());

        Cookie cookie = new Cookie("SESSION_ID", memberSessionDto.sessionId());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1시간
        response.addCookie(cookie);

        return LoginResponse.builder()
                .memberId(memberSessionDto.memberId())
                .nickname(memberSessionDto.nickname())
                .build();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue("SESSION_ID") String sessionId) {
        authService.logout(sessionId);
    }
}
