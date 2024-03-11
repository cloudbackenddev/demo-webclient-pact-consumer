package com.example.pact.demo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDTO(Long id, String name, Integer age, String email, Instant createdAt, Instant updatedAt) {
}
