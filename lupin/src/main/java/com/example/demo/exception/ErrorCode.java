package com.example.demo.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력 값입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 엔티티입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "포인트가 부족합니다."),

    // Feed
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "피드를 찾을 수 없습니다."),
    UNAUTHORIZED_FEED_ACCESS(HttpStatus.FORBIDDEN, "피드에 접근할 권한이 없습니다."),
    ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요를 누른 피드입니다."),

    // Challenge
    CHALLENGE_NOT_FOUND(HttpStatus.NOT_FOUND, "챌린지를 찾을 수 없습니다."),
    CHALLENGE_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "활성화된 챌린지가 아닙니다."),
    ALREADY_JOINED_CHALLENGE(HttpStatus.CONFLICT, "이미 참가한 챌린지입니다."),
    CHALLENGE_CLOSED(HttpStatus.BAD_REQUEST, "종료된 챌린지입니다."),

    // Appointment
    APPOINTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
    APPOINTMENT_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "이미 취소된 예약입니다."),
    APPOINTMENT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 예약입니다."),

    // Lottery
    LOTTERY_TICKET_NOT_FOUND(HttpStatus.NOT_FOUND, "추첨권을 찾을 수 없습니다."),
    LOTTERY_TICKET_ALREADY_USED(HttpStatus.BAD_REQUEST, "이미 사용된 추첨권입니다.");

    private final HttpStatus status;
    private final String message;
}
