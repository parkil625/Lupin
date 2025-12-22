package com.example.demo.scheduler;

import com.example.demo.service.AuctionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class AuctionTaskScheduler {

    private final TaskScheduler taskScheduler;
    private final AuctionService auctionService;

    // '종료 예약증'들을 보관하는 다이어리 (취소할 때 필요해요!)
    // Key: 경매 ID, Value: 예약된 작업(ScheduledFuture)
    private final Map<Long, ScheduledFuture<?>> scheduledEndTasks = new ConcurrentHashMap<>();

    public AuctionTaskScheduler(
            @Qualifier("auctionScheduler") TaskScheduler taskScheduler,
            @Lazy AuctionService auctionService) {
        this.taskScheduler = taskScheduler;
        this.auctionService = auctionService;
    }

    /**
     * 경매 시작 시간을 예약하는 메소드
     * (시작 시간은 보통 잘 안 바뀌어서, 따로 Map에 저장해서 관리하지 않아도 됩니다)
     */
    public void scheduleAuctionStart(Long auctionId, LocalDateTime startTime) {
        Runnable task = () -> {
            log.info("경매 오픈 알람 실행! (Auction ID: {})", auctionId);
            // 해당 시간이 되면 '예정' 상태인 경매들을 '진행 중'으로 바꿔주는 서비스 호출
            auctionService.activateScheduledAuctions(LocalDateTime.now());
        };

        // 스케줄러에게 "이 시간에 실행해줘!" 하고 등록
        taskScheduler.schedule(task, startTime.atZone(ZoneId.systemDefault()).toInstant());

        log.info("경매 시작 예약 완료: ID {}, 시간 {}", auctionId, startTime);
    }

    /**
     * 경매 종료(비활성화) 시간을 예약하는 메소드
     * (입찰이 들어오면 기존 예약을 취소하고 다시 예약해야 하므로 Map 관리가 필수!)
     */
    public void scheduleAuctionEnd(Long auctionId, LocalDateTime endTime) {
        // 1. 이미 잡혀있는 종료 약속이 있다면? -> 취소해라! (시간 연장 로직)
        if (scheduledEndTasks.containsKey(auctionId)) {
            ScheduledFuture<?> existingTask = scheduledEndTasks.get(auctionId);
            existingTask.cancel(false); // "그 약속 취소요!"
            log.info("기존 종료 예약 취소됨 (시간 연장 발생): ID {}", auctionId);
        }

        // 2. 새로운 종료 할 일(Task) 정의
        Runnable task = () -> {
            log.info("경매 종료 알람 실행! (Auction ID: {})", auctionId);
            // 종료 시간이 된 경매들을 찾아서 닫아주는 서비스 호출
            // (초읽기 대상인지 확인하는 로직도 서비스 안에 있으면 같이 처리됨)
            auctionService.closeExpiredAuctions(LocalDateTime.now());

            // 작업이 끝났으니 관리 명단(Map)에서도 지워줌
            scheduledEndTasks.remove(auctionId);
        };

        // 3. 스케줄러에게 새 시간으로 예약 등록
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(
                task,
                endTime.atZone(ZoneId.systemDefault()).toInstant()
        );

        // 4. 나중에 취소할 수 있게 예약증(ScheduledFuture)을 명단에 보관
        scheduledEndTasks.put(auctionId, scheduledTask);

        log.info("경매 종료 예약 완료: ID {}, 시간 {}", auctionId, endTime);
    }
}
