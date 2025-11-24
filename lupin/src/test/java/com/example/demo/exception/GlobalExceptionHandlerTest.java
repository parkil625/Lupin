package com.example.demo.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("BusinessException 처리 - BAD_REQUEST")
    void handleBusinessException_BadRequest() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.INVALID_INPUT_VALUE);

        // when
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("success", false);
        assertThat(response.getBody()).containsEntry("errorCode", "INVALID_INPUT_VALUE");
    }

    @Test
    @DisplayName("BusinessException 처리 - NOT_FOUND")
    void handleBusinessException_NotFound() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.USER_NOT_FOUND);

        // when
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("errorCode", "USER_NOT_FOUND");
    }

    @Test
    @DisplayName("BusinessException 처리 - CONFLICT")
    void handleBusinessException_Conflict() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.DUPLICATE_EMAIL);

        // when
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("errorCode", "DUPLICATE_EMAIL");
    }

    @Test
    @DisplayName("BusinessException 처리 - UNAUTHORIZED")
    void handleBusinessException_Unauthorized() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.INVALID_PASSWORD);

        // when
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("errorCode", "INVALID_PASSWORD");
    }

    @Test
    @DisplayName("BusinessException 처리 - FORBIDDEN")
    void handleBusinessException_Forbidden() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.UNAUTHORIZED_FEED_ACCESS);

        // when
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("errorCode", "UNAUTHORIZED_FEED_ACCESS");
    }

    @Test
    @DisplayName("AccessDeniedException 처리")
    void handleAccessDeniedException() {
        // given
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        // when
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDeniedException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("success", false);
        assertThat(response.getBody()).containsEntry("errorCode", "ACCESS_DENIED");
        assertThat(response.getBody()).containsEntry("message", "해당 기능에 대한 접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("일반 Exception 처리")
    void handleException() {
        // given
        Exception exception = new RuntimeException("Unexpected error");

        // when
        ResponseEntity<Map<String, Object>> response = handler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("success", false);
        assertThat(response.getBody()).containsEntry("message", "서버 오류가 발생했습니다.");
    }

    @Test
    @DisplayName("NullPointerException 처리")
    void handleException_NullPointer() {
        // given
        Exception exception = new NullPointerException("null error");

        // when
        ResponseEntity<Map<String, Object>> response = handler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("다양한 ErrorCode 상태 코드 테스트")
    void handleBusinessException_VariousErrorCodes() {
        // INTERNAL_SERVER_ERROR
        BusinessException serverError = new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(serverError);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        // Feed 관련
        BusinessException feedNotFound = new BusinessException(ErrorCode.FEED_NOT_FOUND);
        response = handler.handleBusinessException(feedNotFound);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Already liked
        BusinessException alreadyLiked = new BusinessException(ErrorCode.ALREADY_LIKED);
        response = handler.handleBusinessException(alreadyLiked);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("OAuth 에러 코드 처리")
    void handleBusinessException_OAuthErrors() {
        // ALREADY_LINKED_OAUTH
        BusinessException linked = new BusinessException(ErrorCode.ALREADY_LINKED_OAUTH);
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(linked);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // OAUTH_TOKEN_ERROR
        BusinessException tokenError = new BusinessException(ErrorCode.OAUTH_TOKEN_ERROR);
        response = handler.handleBusinessException(tokenError);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("에러 응답 구조 검증")
    void errorResponseStructure() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.USER_NOT_FOUND);

        // when
        ResponseEntity<Map<String, Object>> response = handler.handleBusinessException(exception);

        // then
        assertThat(response.getBody()).containsKeys("success", "message", "errorCode");
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("message")).isNotNull();
        assertThat(response.getBody().get("errorCode")).isNotNull();
    }
}
