package com.naroom.api.admin.bootstrap;

import com.naroom.api.admin.domain.entity.AdminRole;
import com.naroom.api.admin.domain.entity.AdminUser;
import com.naroom.api.admin.domain.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 최초 관리자 등록 전용 1회성 명령. 웹 endpoint로 노출하지 않는다(Admin Web Implementation Spec 17.5).
 * 일반 부팅(bootRun 등)에서는 --admin-bootstrap 옵션이 없으면 아무 일도 하지 않고 즉시 반환한다.
 *
 * 사용 예:
 * ./gradlew bootRun --args='--admin-bootstrap --admin-bootstrap-google-sub=<verified-sub> --admin-bootstrap-email=<email> --admin-bootstrap-role=SUPER_ADMIN'
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

	private final AdminUserRepository adminUserRepository;
	private final ConfigurableApplicationContext applicationContext;

	public AdminBootstrapRunner(AdminUserRepository adminUserRepository, ConfigurableApplicationContext applicationContext) {
		this.adminUserRepository = adminUserRepository;
		this.applicationContext = applicationContext;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!args.containsOption("admin-bootstrap")) {
			return;
		}

		int exitCode = execute(args);
		System.exit(SpringApplication.exit(applicationContext, () -> exitCode));
	}

	@Transactional
	int execute(ApplicationArguments args) {
		String googleSub = requireOption(args, "admin-bootstrap-google-sub");
		String email = requireOption(args, "admin-bootstrap-email");
		String roleValue = requireOption(args, "admin-bootstrap-role");
		String displayName = optionalOption(args, "admin-bootstrap-display-name");

		AdminRole role;
		try {
			role = AdminRole.valueOf(roleValue);
		} catch (IllegalArgumentException ex) {
			log.error("[admin-bootstrap] unknown role: {}", roleValue);
			return 1;
		}

		if (adminUserRepository.findByGoogleSub(googleSub).isPresent()) {
			log.info("[admin-bootstrap] admin already registered for this sub - skipping (idempotent)");
			return 0;
		}

		AdminUser adminUser = AdminUser.bootstrap(googleSub, email, displayName, Set.of(role));
		adminUserRepository.save(adminUser);
		log.info("[admin-bootstrap] created admin user with role={}", role);
		return 0;
	}

	private String requireOption(ApplicationArguments args, String name) {
		String value = optionalOption(args, name);
		if (value == null) {
			throw new IllegalArgumentException("Missing required --" + name + " argument for --admin-bootstrap");
		}
		return value;
	}

	private String optionalOption(ApplicationArguments args, String name) {
		List<String> values = args.getOptionValues(name);
		return (values == null || values.isEmpty()) ? null : values.get(0);
	}

}
