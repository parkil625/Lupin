package com.example.demo.scheduler;

import com.example.demo.domain.entity.Challenge;
import com.example.demo.repository.ChallengeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChallengeScheduler {

    private final ChallengeRepository challengeRepository;

    //8시 59분 부터 9시 5초까지 1초마다 돌아가는데 지정된 시간이 됐는데도 시작안하는 이벤트 시작하게 하는 코드
    @Transactional
    @Scheduled(cron = "0/1 * * * * *")
    public void checkChallengeActivation() {

        LocalDateTime now = LocalDateTime.now();

        LocalTime start = LocalTime.of(8, 59, 0);
        LocalTime end = LocalTime.of(9,0,5);

        boolean isMonday = now.getDayOfWeek()== DayOfWeek.MONDAY;

        boolean inWatchWindow =
                !now.toLocalTime().isBefore(start)   // now >= start
                        &&  now.toLocalTime().isBefore(end); // now < end

        if(isMonday &&  inWatchWindow){
            runActivationCheck();
        }

    }

    private void runActivationCheck() {
        List<Challenge> targets = challengeRepository.findScheduledChallengesToOpen(LocalDateTime.now());
        for (Challenge c : targets) c.open();
    }


    @Transactional
    @Scheduled(cron = "0 * * * * *")
    public void checkChallengeClosed() {

        LocalDateTime now = LocalDateTime.now();

        List<Challenge> targets = challengeRepository.findActiveChallengesToClose(now);
        for (Challenge c : targets){
            c.close();
        }
    }
}
