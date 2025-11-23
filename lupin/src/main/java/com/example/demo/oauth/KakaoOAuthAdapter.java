package com.example.demo.oauth;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 카카오 OAuth 어댑터
 */
@Slf4j
@Component
public class KakaoOAuthAdapter implements OAuthProvider {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${KAKAO_CLIENT_ID}")
    private String clientId;

    @Override
    public String getProviderName() {
        return "KAKAO";
    }

    @Override
    public String getAccessToken(String code, String redirectUri) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            if (jsonNode.has("error")) {
                log.error("카카오 토큰 발급 실패: {}", jsonNode.get("error_description").asText());
                throw new BusinessException(ErrorCode.OAUTH_TOKEN_ERROR, "카카오 인증에 실패했습니다.");
            }

            return jsonNode.get("access_token").asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 토큰 발급 중 오류", e);
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_ERROR, "카카오 인증 중 오류가 발생했습니다.");
        }
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    userInfoUrl, HttpMethod.GET, request, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            String id = jsonNode.get("id").asText();
            String nickname = null;
            String email = null;

            if (jsonNode.has("kakao_account")) {
                JsonNode account = jsonNode.get("kakao_account");
                if (account.has("email")) {
                    email = account.get("email").asText();
                }
                if (account.has("profile") && account.get("profile").has("nickname")) {
                    nickname = account.get("profile").get("nickname").asText();
                }
            }

            return new OAuthUserInfo(id, email, nickname);
        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_USER_INFO_ERROR, "카카오 사용자 정보를 가져올 수 없습니다.");
        }
    }
}
