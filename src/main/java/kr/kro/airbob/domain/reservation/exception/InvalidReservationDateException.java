package kr.kro.airbob.domain.reservation.exception;

public class InvalidReservationDateException extends RuntimeException {

    public static final String ERROR_MESSAGE = "체크인 날짜는 체크아웃 날짜보다 빨라야 합니다.";

    public InvalidReservationDateException() {
        super(ERROR_MESSAGE);
    }

    public InvalidReservationDateException(String message) {
        super(message);
    }
}
