package com.anytypeview.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateStudyPauseNoteDTO(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotBlank String reason
) {
}
