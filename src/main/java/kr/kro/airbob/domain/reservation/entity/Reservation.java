package kr.kro.airbob.domain.reservation.entity;

import jakarta.persistence.*;
import kr.kro.airbob.common.domain.BaseEntity;
import kr.kro.airbob.domain.accommodation.entity.Accommodation;
import kr.kro.airbob.domain.member.Member;
import kr.kro.airbob.domain.reservation.common.ReservationStatus;
import kr.kro.airbob.domain.reservation.common.ReservationTime;
import kr.kro.airbob.domain.reservation.dto.ReservationRequestDto;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String message;

	private LocalDateTime checkIn;
	private LocalDateTime checkOut;

	private Integer totalPrice;

	@Enumerated(EnumType.STRING)
	private ReservationStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "accommodation_id")
	private Accommodation accommodation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member guest;

	public static Reservation createReservation(ReservationRequestDto.CreateReservationDto request,
												Accommodation accommodation, Member guest, Integer totalPrice) {
		return Reservation.builder()
				.message(request.getMessage())
				.checkIn(request.getCheckInDate().atTime(ReservationTime.CHECK_IN.getHour(), ReservationTime.CHECK_IN.getMinute()))
				.checkOut(request.getCheckOutDate().atTime(ReservationTime.CHECK_OUT.getHour(), ReservationTime.CHECK_OUT.getMinute()))
				.totalPrice(totalPrice)
				.accommodation(accommodation)
				.guest(guest)
				.status(ReservationStatus.PENDING)
				.build();
	}

}
