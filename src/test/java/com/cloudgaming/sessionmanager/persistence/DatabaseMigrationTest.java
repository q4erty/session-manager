package com.cloudgaming.sessionmanager.persistence;

import com.cloudgaming.sessionmanager.enums.BillingStatus;
import com.cloudgaming.sessionmanager.entity.Game;
import com.cloudgaming.sessionmanager.entity.GameRoom;
import com.cloudgaming.sessionmanager.enums.RoomStatus;
import com.cloudgaming.sessionmanager.repository.GameRepository;
import com.cloudgaming.sessionmanager.repository.GameRoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseMigrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    GameRepository gameRepository;

    @Autowired
    GameRoomRepository gameRoomRepository;

    @Test
    void flywayApplied_gamesTableExists() {
        Long count = gameRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void seed_xonoticExists() {
        List<Game> games = gameRepository.findAll();
        assertThat(games).anyMatch(g ->
                "xonotic".equals(g.getSlug()) &&
                        "Xonotic".equals(g.getTitle()) &&
                        g.getRatePerMinute().compareTo(BigDecimal.ONE) == 0 &&
                        g.getMinBlockMinutes() == 1
        );
    }

    @Test
    void seed_xonoticIsActive() {
        Game xonotic = gameRepository.findBySlug("xonotic").orElseThrow();
        assertThat(xonotic.getIsActive()).isTrue();
    }

    @Test
    void gameRooms_partialUniqueIndex_works() {
        Game game = gameRepository.findBySlug("xonotic").orElseThrow();

        GameRoom room1 = createRoom(game, "owner-1", RoomStatus.ACTIVE);
        gameRoomRepository.saveAndFlush(room1);

        GameRoom room2 = createRoom(game, "owner-1", RoomStatus.TERMINATED);
        gameRoomRepository.saveAndFlush(room2);

        GameRoom room3 = createRoom(game, "owner-1", RoomStatus.ACTIVE);

        assertThatThrownBy(() -> gameRoomRepository.saveAndFlush(room3))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause()
                .hasMessageContaining("uq_game_rooms_one_live_per_owner");
    }

    private GameRoom createRoom(Game game, String ownerSub, RoomStatus status) {
        GameRoom room = new GameRoom();
        try {
            var idField = GameRoom.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(room, UUID.randomUUID());

            var gameField = GameRoom.class.getDeclaredField("game");
            gameField.setAccessible(true);
            gameField.set(room, game);

            var ownerField = GameRoom.class.getDeclaredField("ownerSub");
            ownerField.setAccessible(true);
            ownerField.set(room, ownerSub);

            var statusField = GameRoom.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(room, status);

            var billingField = GameRoom.class.getDeclaredField("billingStatus");
            billingField.setAccessible(true);
            billingField.set(room, BillingStatus.PENDING);

            var rateField = GameRoom.class.getDeclaredField("rateSnapshot");
            rateField.setAccessible(true);
            rateField.set(room, BigDecimal.ONE);

            var minBlockField = GameRoom.class.getDeclaredField("minBlockSnapshot");
            minBlockField.setAccessible(true);
            minBlockField.set(room, 1);

            var maxDurField = GameRoom.class.getDeclaredField("maxDurationSeconds");
            maxDurField.setAccessible(true);
            maxDurField.set(room, 3600);

            var createdField = GameRoom.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(room, Instant.now());
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up GameRoom via reflection", e);
        }
        return room;
    }
}
