package kr.kro.airbob.domain.reservation;

import java.time.LocalDate;
import kr.kro.airbob.common.lock.annotation.DistributedLock;
import kr.kro.airbob.domain.accommodation.entity.Accommodation;
import kr.kro.airbob.domain.accommodation.exception.AccommodationNotFoundException;
import kr.kro.airbob.domain.accommodation.repository.AccommodationRepository;
import kr.kro.airbob.domain.member.Member;
import kr.kro.airbob.domain.member.MemberRepository;
import kr.kro.airbob.domain.member.exception.MemberNotFoundException;
import kr.kro.airbob.domain.reservation.dto.ReservationRequestDto;
import kr.kro.airbob.domain.reservation.dto.ReservationRequestDto.CreateReservationDto;
import kr.kro.airbob.domain.reservation.entity.Reservation;
import kr.kro.airbob.domain.reservation.entity.ReservedDate;
import kr.kro.airbob.domain.reservation.exception.AlreadyReservedException;
import kr.kro.airbob.domain.reservation.exception.InvalidReservationDateException;
import kr.kro.airbob.domain.reservation.repository.ReservationRepository;
import kr.kro.airbob.domain.reservation.repository.ReservedDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservedDateRepository reservedDateRepository;
    private final AccommodationRepository accommodationRepository;
    private final MemberRepository memberRepository;

    @Transactional
    @DistributedLock(key = "#accommodationId", lockName = "reservation")
    public Long createReservation(Long accommodationId, ReservationRequestDto.CreateReservationDto createReservationDto, long memberId) {

        Member guest = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        Accommodation accommodation = accommodationRepository.findById(accommodationId)
                .orElseThrow(AccommodationNotFoundException::new);
        validateReservationAvailability(accommodationId, createReservationDto);

        saveReservedDates(createReservationDto, accommodation);
        Reservation savedReservation = saveReservation(createReservationDto, guest, accommodation);

        return savedReservation.getId();
    }

    //예약된 날짜 저장
    private void saveReservedDates(ReservationRequestDto.CreateReservationDto dto, Accommodation accommodation) {
        long days = calculateReservationDaysCount(dto);

        List<ReservedDate> dates = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            dates.add(ReservedDate.builder()
                    .reservedAt(dto.getCheckInDate().plusDays(i))
                    .accommodation(accommodation)
                    .build());
        }
        reservedDateRepository.saveAll(dates);
    }

    //예약 내역 저장
    private Reservation saveReservation(ReservationRequestDto.CreateReservationDto dto, Member guest, Accommodation accommodation) {
        long days = calculateReservationDaysCount(dto);
        int totalPrice = (int) (days * accommodation.getBasePrice());

        return reservationRepository.save(Reservation.createReservation(dto, accommodation, guest, totalPrice));
    }

    private long calculateReservationDaysCount(CreateReservationDto dto) {
        return ChronoUnit.DAYS.between(dto.getCheckInDate(), dto.getCheckOutDate());
    }

    //숙소가 이미 예약된 날짜가 없는지 검증
    private void validateReservationAvailability(Long accommodationId, CreateReservationDto createReservationDto) {
        List<LocalDate> alreadyReservedDates = reservedDateRepository.findReservedDates(accommodationId, createReservationDto.getCheckInDate(), createReservationDto.getCheckOutDate());

        if (!alreadyReservedDates.isEmpty()) {
            throw new AlreadyReservedException();
        }
    }

    //체크인, 체크아웃 날짜 검증
    public void validReservationDates(CreateReservationDto createReservationDto) {
        if (!createReservationDto.getCheckInDate().isBefore(createReservationDto.getCheckOutDate())) {
            throw new InvalidReservationDateException();
        }
    }

}
