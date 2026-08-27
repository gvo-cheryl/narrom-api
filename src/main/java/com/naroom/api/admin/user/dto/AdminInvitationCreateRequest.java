package com.naroom.api.admin.user.dto;

import com.naroom.api.admin.domain.entity.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record AdminInvitationCreateRequest(@NotBlank @Email String email, @NotEmpty Set<AdminRole> roles) {
}
