package com.example.demo.oauth;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OAuth Provider Factory (Factory + Strategy Pattern)
 * 런타임에 적절한 OAuth 프로바이더를 선택
 */
@Component
public class OAuthProviderFactory {

    private final Map<String, OAuthProvider> providers;

    public OAuthProviderFactory(List<OAuthProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(
                        OAuthProvider::getProviderName,
                        Function.identity()
                ));
    }

    /**
     * 프로바이더 이름으로 어댑터 조회
     */
    public OAuthProvider getProvider(String providerName) {
        OAuthProvider provider = providers.get(providerName.toUpperCase());
        if (provider == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "지원하지 않는 OAuth 프로바이더: " + providerName);
        }
        return provider;
    }

    /**
     * 지원하는 프로바이더 목록 조회
     */
    public List<String> getSupportedProviders() {
        return providers.keySet().stream().sorted().toList();
    }
}
