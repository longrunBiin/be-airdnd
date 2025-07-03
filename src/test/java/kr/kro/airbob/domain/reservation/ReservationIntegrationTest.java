package kr.kro.airbob.domain.reservation;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kr.kro.airbob.domain.accommodation.entity.Accommodation;
import kr.kro.airbob.domain.accommodation.repository.AccommodationRepository;
import kr.kro.airbob.domain.member.Member;
import kr.kro.airbob.domain.member.MemberRepository;
import kr.kro.airbob.domain.reservation.common.ReservationStatus;
import kr.kro.airbob.domain.reservation.dto.ReservationRequestDto;
import kr.kro.airbob.domain.reservation.entity.Reservation;
import kr.kro.airbob.domain.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Transactional
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ReservationIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AccommodationRepository accommodationRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Member testMember;
    private Accommodation testAccommodation;

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("pass");
    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);

        registry.add("spring.flyway.url", mysql::getJdbcUrl);
        registry.add("spring.flyway.user", mysql::getUsername);
        registry.add("spring.flyway.password", mysql::getPassword);

        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }


    @BeforeEach
    void setUp() {
        testMember = memberRepository.save(Member.builder()
                .email("test@example.com")
                .password("pass1234")
                .nickname("tester")
                .build());

        testAccommodation = accommodationRepository.save(Accommodation.builder()
                .name("테스트 숙소")
                .basePrice(10000)
                .build());
    }

    @Test
    @DisplayName("예약에 성공하면 status가 PENDING이 된다.")
    void reservationSuccessBecomeStatusIsPending() {
        // given
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = checkIn.plusDays(2);

        ReservationRequestDto.CreateReservationDto dto = ReservationRequestDto.CreateReservationDto.builder()
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .build();

        // when
        Long reservationId = reservationService.createReservation(testAccommodation.getId(), dto, testMember.getId());

        // then
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("예약이 저장되지 않았습니다"));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
     @DisplayName("한 숙소를 동시에 예약하려고 하면 한명만 예약에 성공해야 한다.")
    void testConcurrentReservationWithDistributedLock() throws InterruptedException {
        // Given
        ReservationRequestDto.CreateReservationDto request = ReservationRequestDto.CreateReservationDto.builder()
                .checkInDate(LocalDate.of(2025, 7, 10))
                .checkOutDate(LocalDate.of(2025, 7, 12))
                .message("동시성 테스트")
                .build();

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    reservationService.createReservation(testAccommodation.getId(), request, testMember.getId());
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // When: 모든 작업이 끝난 뒤, 저장된 예약 수 확인
        long reservationCount = reservationRepository.count();

        // Then: 하나만 저장되어야 함
        assertThat(reservationCount).isEqualTo(1);
    }
}
