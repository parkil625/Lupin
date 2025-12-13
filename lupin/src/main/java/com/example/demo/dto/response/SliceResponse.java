package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.function.Function;

/**
 * 페이지네이션 응답을 위한 공통 DTO
 * Slice 기반의 무한 스크롤 응답에 사용
 */
@Getter
@Builder
@AllArgsConstructor
public class SliceResponse<T> {

    private List<T> content;
    private boolean hasNext;
    private int page;
    private int size;

    /**
     * Slice를 SliceResponse로 변환
     */
    public static <T> SliceResponse<T> from(Slice<T> slice) {
        return SliceResponse.<T>builder()
                .content(slice.getContent())
                .hasNext(slice.hasNext())
                .page(slice.getNumber())
                .size(slice.getSize())
                .build();
    }

    /**
     * Slice의 내용을 변환하여 SliceResponse 생성
     */
    public static <T, R> SliceResponse<R> from(Slice<T> slice, Function<T, R> mapper) {
        List<R> mappedContent = slice.getContent().stream()
                .map(mapper)
                .toList();

        return SliceResponse.<R>builder()
                .content(mappedContent)
                .hasNext(slice.hasNext())
                .page(slice.getNumber())
                .size(slice.getSize())
                .build();
    }

    /**
     * 이미 변환된 컨텐츠 리스트로 SliceResponse 생성
     */
    public static <T> SliceResponse<T> of(List<T> content, boolean hasNext, int page, int size) {
        return SliceResponse.<T>builder()
                .content(content)
                .hasNext(hasNext)
                .page(page)
                .size(size)
                .build();
    }
}
