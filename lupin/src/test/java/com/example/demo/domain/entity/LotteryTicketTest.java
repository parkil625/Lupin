package com.example.demo.domain.entity;

import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LotteryTicket 엔티티 테스트")
class LotteryTicketTest {

    @Test
    @DisplayName("LotteryTicket 생성")
    void create_Success() {
        // given & when
        LotteryTicket ticket = LotteryTicket.builder()
                .id(1L)
                .build();

        // then
        assertThat(ticket.getId()).isEqualTo(1L);
        assertThat(ticket.getUser()).isNull();
    }

    @Test
    @DisplayName("사용자 설정")
    void setUser_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user@test.com")
                .password("password")
                .realName("테스터")
                .role(Role.MEMBER)
                .build();

        LotteryTicket ticket = LotteryTicket.builder()
                .id(1L)
                .build();

        // when
        ticket.setUser(user);

        // then
        assertThat(ticket.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("사용자 변경")
    void setUser_ChangeUser() {
        // given
        User user1 = User.builder()
                .id(1L)
                .userId("user01")
                .email("user01@test.com")
                .password("password")
                .realName("사용자1")
                .role(Role.MEMBER)
                .build();

        User user2 = User.builder()
                .id(2L)
                .userId("user02")
                .email("user02@test.com")
                .password("password")
                .realName("사용자2")
                .role(Role.MEMBER)
                .build();

        LotteryTicket ticket = LotteryTicket.builder()
                .id(1L)
                .build();

        // when
        ticket.setUser(user1);
        ticket.setUser(user2);

        // then
        assertThat(ticket.getUser()).isEqualTo(user2);
    }
}
