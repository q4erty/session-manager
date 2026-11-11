package com.cloudgaming.sessionmanager.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminGameDto(
        UUID id,
        String slug,
        String title,
        String dockerImage,
        BigDecimal ratePerMinute,
        Integer minBlockMinutes,
        Boolean supportsKeyboard,
        Boolean supportsMouse,
        Boolean supportsGamepad,
        Boolean isActive
) {
}
