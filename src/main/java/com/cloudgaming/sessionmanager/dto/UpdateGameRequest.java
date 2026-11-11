package com.cloudgaming.sessionmanager.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateGameRequest(
        @Size(max = 128)
        @Pattern(regexp = ".*\\S.*", flags = Pattern.Flag.DOTALL, message = "must not be blank")
        String title,

        @DecimalMin("0.01")
        @Digits(integer = 13, fraction = 2)
        BigDecimal ratePerMinute,

        @Min(1)
        Integer minBlockMinutes,

        Boolean supportsKeyboard,
        Boolean supportsMouse,
        Boolean supportsGamepad,
        Boolean isActive
) {
}
