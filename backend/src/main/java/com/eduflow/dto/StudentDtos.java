package com.eduflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public final class StudentDtos {

    private StudentDtos() {
    }

    public record StudentRequest(
            @NotBlank @Size(max = 50) String studentNumber,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Email @Size(max = 255) String email,
            @Size(max = 30) String phone,
            @Past LocalDate dateOfBirth
    ) {
    }

    public record StudentResponse(
            Long id,
            String studentNumber,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate dateOfBirth,
            String status,
            long version,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
