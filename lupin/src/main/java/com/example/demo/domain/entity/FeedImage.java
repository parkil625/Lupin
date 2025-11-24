package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ImageType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "feed_images", indexes = {
    @Index(name = "idx_feed_image_feed", columnList = "feedId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FeedImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, insertable = false, updatable = false)
    private Long feedId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedId", nullable = false)
    @Setter
    private Feed feed;

    @Column(name = "s3_key", nullable = false, columnDefinition = "TEXT")
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "img_type", nullable = false, length = 10)
    private ImageType imgType;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    public String getImageUrl() {
        return this.s3Key;
    }
}
