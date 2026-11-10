package com.cloudgaming.sessionmanager.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateGameRequest(
        @NotBlank
        String slug,

        @NotBlank
        String title,

        @NotBlank
        String dockerImage,

        @NotNull
        @Positive
        BigDecimal ratePerMinute,

        @NotNull
        @Min(1)
        Integer minBlockMinutes,

        Boolean supportsKeyboard,
        Boolean supportsMouse,
        Boolean supportsGamepad,
        Boolean isActive
) {
}
