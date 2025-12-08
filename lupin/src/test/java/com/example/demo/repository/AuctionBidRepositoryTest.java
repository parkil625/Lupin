package com.example.demo.repository;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.BidStatus;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.response.AuctionBidResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@DataJpaTest
@ActiveProfiles("test")
class AuctionBidRepositoryTest {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuctionBidRepository auctionBidRepository;

    @Test
    void findBidsByActiveAuction_Success() {
        // Given
        // 1. Create Bidder
        User bidder = User.builder()
                .userId("testBidder")
                .password("password123")
                .name("입찰자") // Check name
                .role(Role.MEMBER)
                .build();
        userRepository.save(bidder);

        // 2. Create ACTIVE Auction (Target)
        Auction activeAuction = Auction.builder()
                .currentPrice(1000L)
                .startTime(LocalDateTime.now().minusHours(1))
                .regularEndTime(LocalDateTime.now().plusHours(1))
                .status(AuctionStatus.ACTIVE)
                .build();
        auctionRepository.save(activeAuction);

        // 3. Create SCHEDULED Auction (Non-Target)
        Auction scheduledAuction = Auction.builder()
                .currentPrice(5000L)
                .startTime(LocalDateTime.now().plusHours(1))
                .regularEndTime(LocalDateTime.now().plusHours(2))
                .status(AuctionStatus.SCHEDULED)
                .build();
        auctionRepository.save(scheduledAuction);

        // 4. Add 3 Bids to ACTIVE Auction (Mixed Order)
        createAndSaveBid(activeAuction, bidder, 1000L);  // 3rd
        createAndSaveBid(activeAuction, bidder, 5000L);  // 1st (Highest)
        createAndSaveBid(activeAuction, bidder, 3000L);  // 2nd

        // 5. Add 1 Bid to SCHEDULED Auction (Should be excluded)
        createAndSaveBid(scheduledAuction, bidder, 99999L);

        // When
        // Now returns List<AuctionBidResponse> instead of Entities
        List<AuctionBid> result = auctionBidRepository.findBidsByActiveAuction();

        // Then
        // 1. Verify Size (Should be 3)
        assertThat(result).hasSize(3);

        // 2. Verify Sorting (Descending Order: 5000 -> 3000 -> 1000)
        assertThat(result.get(0).getBidAmount()).isEqualTo(5000L);
        assertThat(result.get(1).getBidAmount()).isEqualTo(3000L);
        assertThat(result.get(2).getBidAmount()).isEqualTo(1000L);


        assertThat(result.get(0).getUser().getName()).isEqualTo("입찰자");

    }

    // Helper method
    private void createAndSaveBid(Auction auction, User user, Long amount) {
        AuctionBid bid = AuctionBid.builder()
                .auction(auction)
                .user(user)
                .bidAmount(amount)
                .bidTime(LocalDateTime.now())
                .status(BidStatus.ACTIVE)
                .build();
        auctionBidRepository.save(bid);
    }
}
