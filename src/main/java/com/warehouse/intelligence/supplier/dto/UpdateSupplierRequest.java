package com.warehouse.intelligence.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSupplierRequest(
        @NotBlank
        @Size(max = 50)
        String code,

        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @Size(max = 50)
        String phone,

        @Size(max = 255)
        String address
) {}

