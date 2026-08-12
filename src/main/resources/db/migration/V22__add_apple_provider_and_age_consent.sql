-- Beta 1 소셜 로그인 범위에 Apple 로그인이 추가되어 provider 식별자를 확장한다.
ALTER TYPE "social_provider" ADD VALUE 'APPLE';

-- Beta 1은 만 18세 이상만 가입 가능하며 온보딩에서 이 동의를 필수로 받는다.
ALTER TYPE "consent_type" ADD VALUE 'AGE_18_PLUS';
