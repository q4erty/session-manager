package com.cloudgaming.sessionmanager.dto;

import java.math.BigDecimal;

public record GameDto(
        String slug,
        String title,
        BigDecimal ratePerMinute,
        Integer minBlockMinutes,
        Boolean supportsKeyboard,
        Boolean supportsMouse,
        Boolean supportsGamepad
) {
}
