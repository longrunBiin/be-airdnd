package kr.kro.airbob.domain.auth;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import kr.kro.airbob.domain.auth.dto.MemberSessionDto;
import kr.kro.airbob.domain.auth.exception.InvalidPasswordException;
import kr.kro.airbob.domain.auth.exception.NotEqualHostException;
import kr.kro.airbob.domain.member.Member;
import kr.kro.airbob.domain.member.MemberRepository;
import kr.kro.airbob.domain.member.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final SessionRedisRepository sessionRedisRepository;

    public MemberSessionDto login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);

        if (!BCrypt.checkpw(password, member.getPassword())) {
            throw new InvalidPasswordException();
        }

        String sessionId = UUID.randomUUID().toString();
        sessionRedisRepository.saveSession(sessionId, member.getId());

        return new MemberSessionDto(sessionId, member.getId(), member.getNickname());
    }

    public void logout(String sessionId) {
        sessionRedisRepository.deleteSession(sessionId);
    }


    public void validateHost(String sessionId, Long hostId) {
        Long memberId = sessionRedisRepository.getMemberIdBySession(sessionId).orElse(null);

        if (!hostId.equals(memberId)) {
            throw new NotEqualHostException();
        }
    }
}
