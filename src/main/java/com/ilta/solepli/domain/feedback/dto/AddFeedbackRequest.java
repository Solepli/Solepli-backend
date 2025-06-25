package com.ilta.solepli.domain.feedback.dto;

import jakarta.validation.constraints.Size;

public record AddFeedbackRequest(@Size(max = 1000) String feedback) {}
