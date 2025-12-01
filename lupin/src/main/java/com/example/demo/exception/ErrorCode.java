package com.example.demo.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Auth
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Feed
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "피드를 찾을 수 없습니다."),
    FEED_NOT_OWNER(HttpStatus.FORBIDDEN, "피드 수정/삭제 권한이 없습니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_NOT_OWNER(HttpStatus.FORBIDDEN, "댓글 수정/삭제 권한이 없습니다."),

    // Like
    ALREADY_LIKED(HttpStatus.BAD_REQUEST, "이미 좋아요를 눌렀습니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요를 찾을 수 없습니다."),

    // Report
    ALREADY_REPORTED(HttpStatus.BAD_REQUEST, "이미 신고한 콘텐츠입니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),

    // OAuth
    OAUTH_NOT_LINKED(HttpStatus.BAD_REQUEST, "연동된 소셜 계정이 아닙니다."),
    OAUTH_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "소셜 인증 토큰 오류가 발생했습니다."),
    ALREADY_LINKED_OAUTH(HttpStatus.BAD_REQUEST, "이미 연동된 소셜 계정입니다."),
    OAUTH_ALREADY_USED(HttpStatus.BAD_REQUEST, "이미 다른 계정에 연동된 소셜 계정입니다."),
    OAUTH_USER_INFO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "소셜 사용자 정보를 가져올 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
