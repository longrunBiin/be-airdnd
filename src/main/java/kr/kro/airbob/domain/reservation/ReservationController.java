package kr.kro.airbob.domain.reservation;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import kr.kro.airbob.domain.reservation.dto.ReservationRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations/accommodations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping("/{accommodationId}")
    public ResponseEntity<Map<String,Long>> createReservation(@PathVariable Long accommodationId,
                                                              @RequestBody ReservationRequestDto.CreateReservationDto createReservationDto,
                                                              HttpServletRequest request) {

        long memberId = (long) request.getAttribute("memberId");

        reservationService.validReservationDates(createReservationDto);
        Long reservationId = reservationService.createReservation(accommodationId, createReservationDto, memberId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", reservationId));
    }

}
