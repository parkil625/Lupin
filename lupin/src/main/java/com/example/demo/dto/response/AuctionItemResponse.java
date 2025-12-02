package com.example.demo.dto.response;

import com.example.demo.domain.entity.AuctionItem;

public record AuctionItemResponse(
        Long itemId,
        String itemName,
        String description,
        String imageUrl
) {
    public static AuctionItemResponse from(AuctionItem item) {
        return new AuctionItemResponse(
                item.getId(),
                item.getItemName(),
                item.getDescription(),
                item.getItemImage()
        );
    }
}
