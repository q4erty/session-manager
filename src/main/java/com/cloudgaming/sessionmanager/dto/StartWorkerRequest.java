package com.cloudgaming.sessionmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StartWorkerRequest(
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[a-z0-9-]+$", message = "must contain only lowercase letters, digits and dashes")
        String gameSlug
) {
}
