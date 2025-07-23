package kr.kro.airbob.domain.accommodation.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccommodationCreatedListener {

    private final RedisTemplate<String, Object> redisTemplate;

    @Async
    @EventListener
    public void handle(AccommodationCreatedEvent accommodation) {
        // 캐시 저장
        redisTemplate.opsForList().leftPush("recent:accommodations", accommodation);

        // 리스트가 너무 길어지지 않도록 트림
        redisTemplate.opsForList().trim("recent:accommodations", 0, 99); // 최대 100개 유지
    }
}
