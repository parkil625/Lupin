package com.example.demo.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Auth
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_BANNED(HttpStatus.FORBIDDEN, "정지된 계정입니다."),
    // [추가] 기능 제한 에러 코드
    FEED_CREATION_RESTRICTED(HttpStatus.FORBIDDEN, "피드 작성 금지 패널티가 적용 중입니다."),
    COMMENT_CREATION_RESTRICTED(HttpStatus.FORBIDDEN, "댓글 작성 금지 패널티가 적용 중입니다."),
    
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    SESSION_EXPIRED_BY_OTHER_LOGIN(HttpStatus.UNAUTHORIZED, "다른 기기에서 로그인하여 현재 세션이 만료되었습니다."),
    
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Feed
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "피드를 찾을 수 없습니다."),
    FEED_NOT_OWNER(HttpStatus.FORBIDDEN, "피드 수정/삭제 권한이 없습니다."),
    FEED_IMAGES_REQUIRED(HttpStatus.BAD_REQUEST, "시작 사진과 끝 사진을 모두 업로드해주세요."),
    FEED_INVALID_PHOTO_TIME(HttpStatus.BAD_REQUEST, "시작 사진의 시간이 끝 사진보다 같거나 늦습니다. 올바른 사진을 업로드해주세요."),
    FEED_PHOTO_TIME_NOT_FOUND(HttpStatus.BAD_REQUEST, "사진에서 촬영 시간 정보를 찾을 수 없습니다. EXIF 정보가 포함된 사진을 업로드해주세요."),
    FEED_WORKOUT_TOO_LONG(HttpStatus.BAD_REQUEST, "운동 시간이 24시간을 초과합니다. 올바른 사진을 업로드해주세요."),
    FEED_PHOTO_NOT_TODAY(HttpStatus.BAD_REQUEST, "당일 촬영한 사진만 업로드할 수 있습니다. 오늘 운동한 사진을 업로드해주세요."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    COMMENT_NOT_OWNER(HttpStatus.FORBIDDEN, "댓글 수정/삭제 권한이 없습니다."),
    REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글에는 답글을 달 수 없습니다."),

    // Like
    ALREADY_LIKED(HttpStatus.BAD_REQUEST, "이미 좋아요를 눌렀습니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요를 찾을 수 없습니다."),

    // Report
    ALREADY_REPORTED(HttpStatus.BAD_REQUEST, "이미 신고한 콘텐츠입니다."),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),

    // Appointment
    APPOINTMENT_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 예약된 시간입니다."),
    APPOINTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."),
    APPOINTMENT_CANCELLED(HttpStatus.BAD_REQUEST, "취소된 예약입니다."),
    APPOINTMENT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "이미 진행 중인 예약입니다."),
    APPOINTMENT_COMPLETED(HttpStatus.BAD_REQUEST, "이미 완료된 예약입니다."),

    // OAuth
    OAUTH_NOT_LINKED(HttpStatus.BAD_REQUEST, "연동된 소셜 계정이 아닙니다."),
    OAUTH_TOKEN_ERROR(HttpStatus.UNAUTHORIZED, "소셜 인증 토큰 오류가 발생했습니다."),
    ALREADY_LINKED_OAUTH(HttpStatus.BAD_REQUEST, "이미 연동된 소셜 계정입니다."),
    OAUTH_ALREADY_USED(HttpStatus.BAD_REQUEST, "이미 다른 계정에 연동된 소셜 계정입니다."),
    OAUTH_USER_INFO_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "소셜 사용자 정보를 가져올 수 없습니다."),

    // Auction
    INVALID_BID_AMOUNT(HttpStatus.BAD_REQUEST, "입찰 금액이 유효하지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
