package com.ilta.solepli.domain.feedback.dto;

import jakarta.validation.constraints.Size;

public record AddFeedbackRequest(
    @Size(max = 1000, message = "글자수는 최대 1000자 입니다.") String feedback) {}
