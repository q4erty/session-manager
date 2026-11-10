package com.cloudgaming.sessionmanager.repository;

import com.cloudgaming.sessionmanager.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {
    List<Game> findByIsActiveTrue();
    boolean existsBySlug(String slug);
    Optional<Game> findBySlug(String slug);
}
