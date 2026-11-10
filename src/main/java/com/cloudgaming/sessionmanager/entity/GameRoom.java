package com.cloudgaming.sessionmanager.entity;

import com.cloudgaming.sessionmanager.enums.RoomStatus;
import com.cloudgaming.sessionmanager.enums.BillingStatus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "game_rooms")
public class GameRoom {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "owner_sub", nullable = false)
    private String ownerSub;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private RoomStatus status;

    @Column(name = "billing_status", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private BillingStatus billingStatus;

    @Column(name = "end_reason", length = 16)
    private String endReason;

    @Column(name = "container_id", length = 80)
    private String containerId;

    @Column(name = "worker_ip", length = 45)
    private String workerIp;

    @Column(name = "webrtc_port")
    private Integer webrtcPort;

    @Column(name = "rate_snapshot", nullable = false, precision = 15, scale = 2)
    private BigDecimal rateSnapshot;

    @Column(name = "min_block_snapshot", nullable = false)
    private Integer minBlockSnapshot;

    @Column(name = "max_duration_seconds", nullable = false)
    private Integer maxDurationSeconds;

    @Column(name = "billed_minutes")
    private Integer billedMinutes;

    @Column(name = "billed_amount", precision = 15, scale = 2)
    private BigDecimal billedAmount;

    @Column(name = "debit_transaction_id")
    private UUID debitTransactionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "last_healthy_at")
    private Instant lastHealthyAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameRoom that = (GameRoom) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
