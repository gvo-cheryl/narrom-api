package com.naroom.api.admin.auth;

import com.naroom.api.admin.domain.entity.AdminRole;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// 역할을 "ROLE_" 접두사가 붙은 GrantedAuthority로 노출해 hasRole()/@PreAuthorize와 바로 맞물리게 한다.
public class AdminAuthentication extends AbstractAuthenticationToken {

	private final UUID adminUserId;
	private final UUID adminSessionId;

	public AdminAuthentication(UUID adminUserId, UUID adminSessionId, Set<AdminRole> roles) {
		super(roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.name())).collect(Collectors.toSet()));
		this.adminUserId = adminUserId;
		this.adminSessionId = adminSessionId;
		setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return null;
	}

	@Override
	public Object getPrincipal() {
		return adminUserId;
	}

	public UUID getAdminUserId() {
		return adminUserId;
	}

	public UUID getAdminSessionId() {
		return adminSessionId;
	}

}
