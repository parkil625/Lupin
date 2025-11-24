package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ReportTargetType;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Report 엔티티 테스트")
class ReportTest {

    @Test
    @DisplayName("피드 신고 생성")
    void createFeedReport_Success() {
        // given
        User reporter = User.builder()
                .id(1L)
                .userId("reporter")
                .email("reporter@test.com")
                .password("password")
                .realName("신고자")
                .role(Role.MEMBER)
                .build();

        // when
        Report report = Report.builder()
                .id(1L)
                .targetType(ReportTargetType.FEED)
                .targetId(100L)
                .reporter(reporter)
                .build();

        // then
        assertThat(report.getId()).isEqualTo(1L);
        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.FEED);
        assertThat(report.getTargetId()).isEqualTo(100L);
        assertThat(report.getReporter()).isEqualTo(reporter);
    }

    @Test
    @DisplayName("댓글 신고 생성")
    void createCommentReport_Success() {
        // given
        User reporter = User.builder()
                .id(1L)
                .userId("reporter")
                .email("reporter@test.com")
                .password("password")
                .realName("신고자")
                .role(Role.MEMBER)
                .build();

        // when
        Report report = Report.builder()
                .id(2L)
                .targetType(ReportTargetType.COMMENT)
                .targetId(200L)
                .reporter(reporter)
                .build();

        // then
        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.COMMENT);
        assertThat(report.getTargetId()).isEqualTo(200L);
    }
}
