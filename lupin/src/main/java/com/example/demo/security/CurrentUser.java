package com.example.demo.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 메서드 파라미터에 현재 로그인한 사용자를 주입하는 어노테이션
 *
 * 사용 예:
 * @GetMapping("/me")
 * public ResponseEntity<?> getMyInfo(@CurrentUser User user) { ... }
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
