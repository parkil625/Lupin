package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ImageType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "feed_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FeedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @Column(name = "s3_key", nullable = false, columnDefinition = "TEXT")
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "img_type", nullable = false, length = 10)
    private ImageType imgType;

    @Column(name = "captured_at")
    private java.time.LocalDateTime capturedAt;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
