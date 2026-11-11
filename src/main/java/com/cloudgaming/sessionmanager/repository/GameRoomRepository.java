package com.cloudgaming.sessionmanager.repository;

import com.cloudgaming.sessionmanager.entity.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface GameRoomRepository extends JpaRepository<GameRoom, UUID> {
}
