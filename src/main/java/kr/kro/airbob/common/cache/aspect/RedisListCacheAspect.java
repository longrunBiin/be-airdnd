package kr.kro.airbob.common.cache.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import kr.kro.airbob.common.cache.annotaion.RedisListCacheable;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class RedisListCacheAspect {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Around("@annotation(redisListCacheable)")
    public Object around(ProceedingJoinPoint joinPoint, RedisListCacheable redisListCacheable) throws Throwable {
        String key = redisListCacheable.key();
        int limit = redisListCacheable.limit();
        Class<?> type = redisListCacheable.type();

        // 1. Redis 리스트 조회
        List<String> cachedJsonList = redisTemplate.opsForList().range(key, 0, limit - 1);
        if (cachedJsonList != null && !cachedJsonList.isEmpty()) {
            List<Object> result = new ArrayList<>();
            for (String json : cachedJsonList) {
                result.add(objectMapper.readValue(json, type));
            }
            return result;
        }

        // 2. 실제 메소드 실행 → DB 조회
        Object data = joinPoint.proceed();

        // 3. 캐시에 저장
        if (data instanceof List<?> list) {
            List<String> serialized = new ArrayList<>();
            for (Object item : list) {
                serialized.add(objectMapper.writeValueAsString(item));
            }

            if (!serialized.isEmpty()) {
                redisTemplate.delete(key);
                redisTemplate.opsForList().leftPushAll(key, serialized);
                redisTemplate.opsForList().trim(key, 0, limit - 1);
            }
        }

        return data;
    }
}
