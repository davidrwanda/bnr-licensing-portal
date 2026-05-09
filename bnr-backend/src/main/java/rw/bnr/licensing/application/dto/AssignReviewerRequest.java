package rw.bnr.licensing.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * @author David NTAMAKEMWA
 */
public record AssignReviewerRequest(@NotNull UUID reviewerId) {}
