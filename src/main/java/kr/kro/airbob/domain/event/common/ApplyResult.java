package kr.kro.airbob.domain.event.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@AllArgsConstructor
public enum ApplyResult {
    SUCCESS("응모에 성공하였습니다.", HttpStatus.OK),
    DUPLICATE("이미 응모한 사용자입니다.", HttpStatus.CONFLICT),
    FULL("이벤트 응모가 마감되었습니다.", HttpStatus.GONE),
    ERROR("에러가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private String message;
    private HttpStatus status;

    public ResponseEntity<String> toResponse() {
        return ResponseEntity.status(status).body(message);
    }
}
