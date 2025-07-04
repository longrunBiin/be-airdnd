package kr.kro.airbob.domain.auth.dto;

public record MemberSessionDto(String sessionId, long memberId, String nickname) {}
