package com.cloudgaming.sessionmanager.dto;

import java.math.BigDecimal;

public record UpdateGameRequest(
        String title,
        BigDecimal ratePerMinute,
        Integer minBlockMinutes,
        Boolean supportsKeyboard,
        Boolean supportsMouse,
        Boolean supportsGamepad,
        Boolean isActive
) {
}
