package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "auction_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
public class AuctionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String itemName;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String itemImage;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;
}
