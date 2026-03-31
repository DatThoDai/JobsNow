package com.JobsNow.backend.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentRequest {
    @NotNull
    private Integer planId;

    private Integer jobId;
}
