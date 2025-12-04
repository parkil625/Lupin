package com.example.demo.controller;

import com.example.demo.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auction")
@RequiredArgsConstructor
public class AuctionContoller {
    private final AuctionService auctionService;

}
