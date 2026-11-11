package com.cloudgaming.sessionmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "games")
public class Game {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(name = "docker_image", nullable = false)
    private String dockerImage;

    @Column(name = "rate_per_minute", nullable = false, precision = 15, scale = 2)
    private BigDecimal ratePerMinute;

    @Column(name = "min_block_minutes", nullable = false)
    private Integer minBlockMinutes;

    @Column(name = "supports_keyboard", nullable = false)
    private Boolean supportsKeyboard;

    @Column(name = "supports_mouse", nullable = false)
    private Boolean supportsMouse;

    @Column(name = "supports_gamepad", nullable = false)
    private Boolean supportsGamepad;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    protected Game() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return Objects.equals(id, game.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
