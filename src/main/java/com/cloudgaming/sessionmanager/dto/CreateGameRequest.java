package com.cloudgaming.sessionmanager.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateGameRequest(
        @NotBlank
        @Size(max = 64)
        String slug,

        @NotBlank
        @Size(max = 128)
        String title,

        @NotBlank
        @Size(max = 255)
        String dockerImage,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
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
