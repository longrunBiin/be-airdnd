package kr.kro.airbob.domain.event;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import kr.kro.airbob.domain.event.common.ApplyResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


@SpringBootTest
class EventServiceTest {

    @Autowired
    private EventService eventService;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("레디스 요청이 실패한 경우 서킷브레이커로 인해 fallback 메소드가 실행된다")
    void circuitBreaker_opens_after_consecutive_failures() {
        // given
        given(redisTemplate.execute(any(), anyList(), any(), any()))
                .willThrow(new RedisConnectionFailureException("Redis down"));

        // when
        ApplyResult result = eventService.applyToEvent(1L, 999L, 5);

        // then: fallback이 호출되어 ERROR 반환
        assertThat(result).isEqualTo(ApplyResult.ERROR);
    }
}
