package kr.kro.airbob.domain.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import kr.kro.airbob.domain.accommodation.entity.Accommodation;
import kr.kro.airbob.domain.accommodation.exception.AccommodationNotFoundException;
import kr.kro.airbob.domain.accommodation.repository.AccommodationRepository;
import kr.kro.airbob.domain.member.Member;
import kr.kro.airbob.domain.member.MemberRepository;
import kr.kro.airbob.domain.member.exception.MemberNotFoundException;
import kr.kro.airbob.domain.reservation.dto.ReservationRequestDto;
import kr.kro.airbob.domain.reservation.entity.Reservation;
import kr.kro.airbob.domain.reservation.entity.ReservedDate;
import kr.kro.airbob.domain.reservation.exception.AlreadyReservedException;
import kr.kro.airbob.domain.reservation.exception.InvalidReservationDateException;
import kr.kro.airbob.domain.reservation.repository.ReservationRepository;
import kr.kro.airbob.domain.reservation.repository.ReservedDateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReservationServiceTest {

    @InjectMocks
    private ReservationService reservationService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AccommodationRepository accommodationRepository;

    @Mock
    private ReservedDateRepository reservedDateRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("이미 예약된 날짜에 예약을 시도하면 예외가 발생하고 예약내역이 저장되지 않는다.")
    void createReservation_whenDatesAlreadyReserved() {
        // given
        Long accommodationId = 1L;
        LocalDate checkIn = LocalDate.of(2025, 6, 20);
        LocalDate checkOut = LocalDate.of(2025, 6, 22);

        ReservationRequestDto.CreateReservationDto dto = ReservationRequestDto.CreateReservationDto.builder()
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .message("테스트 메시지")
                .build();

        Member guest = mock(Member.class);
        Accommodation accommodation = mock(Accommodation.class);
        List<LocalDate> reservedDates = List.of(
                LocalDate.of(2025, 6, 20),
                LocalDate.of(2025, 6, 20)
        );

        given(memberRepository.findById(anyLong())).willReturn(Optional.of(guest));
        given(accommodationRepository.findById(accommodationId)).willReturn(Optional.of(accommodation));
        given(reservedDateRepository.findReservedDates(accommodationId, checkIn, checkOut)).willReturn(reservedDates);

        // when & then
        assertThrows(AlreadyReservedException.class, () -> {
            reservationService.createReservation(accommodationId, dto, anyLong());
        });

        then(reservationRepository).should(never()).save(any());
        then(reservedDateRepository).should(never()).saveAll(any());
    }

    @Test
    @DisplayName("예약되지 않은 날짜의 숙소는 예약 내역을 생성한다.")
    void createReservation_whenDatesAvailable() {
        // given
        Long accommodationId = 1L;
        Long reservationId = 1L;
        LocalDate checkIn = LocalDate.of(2025, 6, 20);
        LocalDate checkOut = LocalDate.of(2025, 6, 22);

        ReservationRequestDto.CreateReservationDto dto = ReservationRequestDto.CreateReservationDto.builder()
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .message("테스트 메시지")
                .build();

        Member guest = mock(Member.class);
        Accommodation accommodation = mock(Accommodation.class);
        Reservation reservation = mock(Reservation.class);

        given(accommodation.getBasePrice()).willReturn(10000);
        given(reservation.getId()).willReturn(reservationId);

        given(memberRepository.findById(anyLong())).willReturn(Optional.of(guest));
        given(accommodationRepository.findById(accommodationId)).willReturn(Optional.of(accommodation));
        given(reservedDateRepository.findReservedDates(accommodationId, checkIn, checkOut)).willReturn(List.of());
        given(reservationRepository.save(any())).willReturn(reservation);

        // when
        Long savedReservationId = reservationService.createReservation(accommodationId, dto, anyLong());

        // then
        then(reservationRepository).should().save(any(Reservation.class));
        assertThat(savedReservationId).isEqualTo(reservation.getId());

        // ArgumentCaptor를 사용해 saveAll에 전달된 리스트를 검증
        ArgumentCaptor<List<ReservedDate>> captor = ArgumentCaptor.forClass(List.class);
        then(reservedDateRepository).should().saveAll(captor.capture());

        List<ReservedDate> savedDates = captor.getValue();
        long expectedDays = ChronoUnit.DAYS.between(checkIn, checkOut);
        assertThat(savedDates).hasSize((int) expectedDays);

        for (int i = 0; i < savedDates.size(); i++) {
            ReservedDate rd = savedDates.get(i);
            assertThat(rd.getReservedAt()).isEqualTo(checkIn.plusDays(i));
            assertThat(rd.getAccommodation()).isEqualTo(accommodation);
        }
    }

    @Test
    @DisplayName("존재하지 않는 유저가 예약하려고 하면 예외가 발생해야 한다.")
    void createReservation_shouldThrowException_whenMemberNotFound()  {
        // given
        long memberId = 1L;
        Long accommodationId = 1L;
        ReservationRequestDto.CreateReservationDto request = ReservationRequestDto.CreateReservationDto.builder()
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(2))
                .build();

        given(memberRepository.findById(memberId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(accommodationId, request,  memberId))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("존재하지 않는 숙소를 예약하려고 하면 예외가 발생해야 한다.")
    void createReservation_shouldThrowException_whenAccommodationNotFound()  {
        // given
        long memberId = 1L;
        Long accommodationId = 1L;
        ReservationRequestDto.CreateReservationDto request = ReservationRequestDto.CreateReservationDto.builder()
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(2))
                .build();

        Member member = mock(Member.class);

        given(memberRepository.findById(memberId))
                .willReturn(Optional.of(member));

        given(accommodationRepository.findById(accommodationId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reservationService.createReservation(accommodationId, request, memberId))
                .isInstanceOf(AccommodationNotFoundException.class)
                .hasMessage("존재하지 않는 숙소입니다.");
    }

    @DisplayName("예약 요청 시 체크인과 체크아웃 날짜가 잘못되면 예외가 발생한다")
    @ParameterizedTest
    @CsvSource({
            "2025-07-03,2025-07-03",  // check-in == check-out
            "2025-07-05,2025-07-03"   // check-in > check-out
    })
    void invalidReservationDate_throwsException(LocalDate checkIn, LocalDate checkOut) {
        // given
        ReservationRequestDto.CreateReservationDto dto = ReservationRequestDto.CreateReservationDto.builder()
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .build();

        // when & then
        assertThrows(InvalidReservationDateException.class, () -> {
            reservationService.createReservation(1L, dto, 1L);
        });
    }

}
