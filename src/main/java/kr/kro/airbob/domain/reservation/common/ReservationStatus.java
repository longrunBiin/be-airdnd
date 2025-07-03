package kr.kro.airbob.domain.reservation.common;

public enum ReservationStatus {
    PENDING,        // 결제 전 임시 예약
    CONFIRMED,      // 결제 완료, 예약 확정
    CANCELLED,      // 사용자 또는 관리자에 의해 취소됨
    COMPLETED,      // 숙박 완료
    NO_SHOW         // 사용자가 체크인하지 않음
}
