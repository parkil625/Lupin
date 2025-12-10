package com.example.demo.exception;

import com.example.demo.dto.response.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException; // ⭐️ 추가됨
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BusinessException 처리 - ErrorCode의 상태와 메시지 반환")
    void handleBusinessException() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.USER_NOT_FOUND);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("BusinessException 처리 - 커스텀 메시지")
    void handleBusinessExceptionWithCustomMessage() {
        // given
        BusinessException exception = new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이메일 형식이 올바르지 않습니다.");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_INPUT_VALUE");
        assertThat(response.getBody().getMessage()).isEqualTo("이메일 형식이 올바르지 않습니다.");
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 처리 - 유효성 검증 실패")
    void handleMethodArgumentNotValidException() {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("request", "content", "내용은 필수입니다.");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INVALID_INPUT_VALUE");
        assertThat(response.getBody().getMessage()).isEqualTo("내용은 필수입니다.");
    }

    // ⭐️ 추가됨: 로그인 실패 예외 테스트
    @Test
    @DisplayName("BadCredentialsException 처리 - 401 Unauthorized 반환")
    void handleBadCredentialsException() {
        // given
        BadCredentialsException exception = new BadCredentialsException("자격 증명 실패");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleBadCredentialsException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("LOGIN_FAILED");
        assertThat(response.getBody().getMessage()).isEqualTo("아이디 또는 비밀번호가 일치하지 않습니다.");
    }

    // ⭐️ 수정됨: 디버깅 메시지 포맷에 맞춰 기대값 변경
    @Test
    @DisplayName("일반 Exception 처리 - 500 에러 (디버깅 메시지 포함)")
    void handleException() {
        // given
        Exception exception = new RuntimeException("예상치 못한 오류");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleException(exception);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        
        // 현재 GlobalExceptionHandler가 반환하는 실제 포맷으로 검증
        assertThat(response.getBody().getMessage()).isEqualTo("서버 에러 [RuntimeException]: 예상치 못한 오류");
    }
}