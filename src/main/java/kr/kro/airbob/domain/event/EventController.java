package kr.kro.airbob.domain.event;

import static kr.kro.airbob.domain.event.common.ApplyResult.DUPLICATE;
import static kr.kro.airbob.domain.event.common.ApplyResult.FULL;
import static kr.kro.airbob.domain.event.common.ApplyResult.SUCCESS;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import kr.kro.airbob.domain.event.common.ApplyResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/event")
public class EventController {

    private final EventService eventService;

    private record MemberRequest(Long memberId) {}

    @PostMapping("/{eventId}")
    public ResponseEntity<String> applyEvent(@PathVariable Long eventId, @RequestBody MemberRequest request) {
        if (eventService.isEventFull(eventId)) {
            return ResponseEntity.status(HttpStatus.GONE).body(FULL.getMessage());
        }

        int eventMaxParticipants = eventService.getEventMaxParticipants(eventId);
        ApplyResult applyResult = eventService.applyToEvent(eventId, request.memberId, eventMaxParticipants);

        return returnResponseByApplyResult(eventId, applyResult);
    }

    private ResponseEntity<String> returnResponseByApplyResult(Long eventId, ApplyResult applyResult) {
        if (applyResult == FULL && eventService.markEventFullIfAbsent(eventId)){
            eventService.publishEventQueueIsFull(eventId);
        }

        return applyResult.toResponse();
    }
}
