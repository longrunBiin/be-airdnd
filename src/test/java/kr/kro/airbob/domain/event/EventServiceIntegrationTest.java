package kr.kro.airbob.domain.event;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kr.kro.airbob.domain.event.common.ApplyResult;
import kr.kro.airbob.domain.event.common.EventStatus;
import kr.kro.airbob.domain.event.entity.Event;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Transactional
@Testcontainers
class EventServiceIntegrationTest {

    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void setRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    private EventService eventService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static DefaultRedisScript<String> applyEventScript;

    private static final String APPLY_EVENT_SCRIPT = """
        local isAlreadyApplied = redis.call("SISMEMBER", KEYS[1], ARGV[1])
        if isAlreadyApplied == 1 then
            return "duplicate"
        end
                    
        local currentQueueSize = redis.call("LLEN", KEYS[2])
        local maxQueueSize = tonumber(ARGV[2])
        if currentQueueSize >= maxQueueSize then
            return "full"
        end
                    
        redis.call("SADD", KEYS[1], ARGV[1])
        redis.call("RPUSH", KEYS[2], ARGV[1])
        return "success"
        """;

    @BeforeAll
    static void setup() {
        applyEventScript = new DefaultRedisScript<>();
        applyEventScript.setScriptText(APPLY_EVENT_SCRIPT);
        applyEventScript.setResultType(String.class);
    }

    @BeforeEach
    void clearRedis() {
        redisTemplate.delete(Arrays.asList("event:1:set", "event:1:zset"));
    }

    @Test
    @DisplayName("동시에 중복된 유저가 응모할 때 하나만 성공해야 한다")
    void 동시에_중복된_유저가_응모할_때_하나만_성공해야_한다() throws InterruptedException {
        //given
        Long eventId = 1L;
        Long memberId = 42L;
        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<ApplyResult> results = Collections.synchronizedList(new ArrayList<>());

        //when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ApplyResult result = eventService.applyToEvent(eventId, memberId, 100);
                results.add(result);
                latch.countDown();
            });
        }

        latch.await();

        //then
        long successCount = results.stream().filter(r -> r == ApplyResult.SUCCESS).count();
        long duplicateCount = results.stream().filter(r -> r == ApplyResult.DUPLICATE).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(duplicateCount).isEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("최대 인원 수를 초과한 요청이 와도 zset에는 최대 인원 수만 저장된다")
    void zsetShouldNotExceedMaxParticipants() throws InterruptedException {
        // given
        Long eventId = 1L;
        int maxParticipants = 100;
        int requestCount = 150;

        // when
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(requestCount);
        for (int i = 0; i < requestCount; i++) {
            final long memberId = i + 1;
            executor.submit(() -> {
                eventService.applyToEvent(eventId, memberId, maxParticipants);
                latch.countDown();
            });
        }

        latch.await();

        // then
        Set<String> members = redisTemplate.opsForZSet().range("event:" + eventId + ":zset", 0, -1);
        assertThat(members.size()).isEqualTo(maxParticipants);
    }

    @Test
    @DisplayName("선착순 인원에 들면 응모가 성공한다.")
    void applyEventSuccess() {
        //given & when
        String result = redisTemplate.execute(applyEventScript,
                List.of("event:1:set", "event:1:queue"),
                "1", "10000");

        //then
        assertThat(result).isEqualTo("success");
        assertThat(redisTemplate.opsForSet().isMember("event:1:set", "1")).isTrue();
        assertThat(redisTemplate.opsForList().range("event:1:queue", 0, -1)).isEqualTo(List.of("1"));
    }

    @Test
    @DisplayName("같은 사용자가 중복으로 응모하면 duplicate를 응답한다.")
    void duplicateUserApplyTest() {
        //given
        redisTemplate.opsForSet().add("event:1:set", "1");

        //when
        String result = redisTemplate.execute(applyEventScript,
                List.of("event:1:set", "event:1:queue"),
                "1", "10000");

        //then
        assertThat(result).isEqualTo("duplicate");
    }

    @Test
    @DisplayName("이벤트의 정원이 초과되면 full을 응답한다.")
    void participantsExceedLimitTest() {
        //given
        for (int i = 0; i < 10000; i++) {
            redisTemplate.opsForList().rightPush("event:1:queue", String.valueOf(i));
        }

        //when
        String result = redisTemplate.execute(applyEventScript,
                List.of("event:1:set", "event:1:queue"),
                "99999", "10000");

        //then
        assertThat(result).isEqualTo("full");
    }
}
