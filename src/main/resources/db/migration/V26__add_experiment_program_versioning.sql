-- 작은 실험 프로그램(코스) 관리자 편집(관리자 웹 5단계, §8.5/8.6). record_prompts(V24)/quotes(V25)와 같은
-- 패턴: 발행본은 절대 UPDATE하지 않고 같은 code의 새 row(content_version 증가)로 바꾼다.
-- user_experiment_programs는 이미 program_id+program_version으로 자기가 시작한 버전을 그대로 참조하므로
-- (V15) 코스를 새 버전으로 바꿔도 진행 중인 사용자의 기록은 영향받지 않는다.
--
-- 낙관적 잠금은 별도 컬럼을 추가하지 않고 기존 content_version을 그대로 재사용한다 - 콘텐츠 버전 번호와
-- 동시 편집 충돌 감지를 같은 값으로 취급하는 의도적 단순화다(엔티티 @Version 매핑에서 처리).

-- code 단일 UNIQUE는 "발행본은 그대로 두고 새 row를 추가"하는 모델과 충돌한다 - 같은 code로 두 번째
-- row를 넣을 수 없기 때문이다. (code, content_version) 복합 UNIQUE로 완화한다.
ALTER TABLE "experiment_programs" DROP CONSTRAINT "experiment_programs_code_key";
ALTER TABLE "experiment_programs"
  ADD CONSTRAINT "uk_experiment_programs_code_content_version" UNIQUE ("code", "content_version");

ALTER TABLE "experiment_programs"
  ADD COLUMN "supersedes_program_id" uuid REFERENCES "experiment_programs" ("id"),
  ADD COLUMN "created_by_admin_id" uuid REFERENCES "admin_users" ("id");
COMMENT ON COLUMN "experiment_programs"."supersedes_program_id" IS '이 버전이 대체한 이전 버전';
COMMENT ON COLUMN "experiment_programs"."created_by_admin_id" IS '이 버전을 만든 관리자; 초기 시드 데이터는 NULL';

-- 같은 code에서 PUBLISHED는 동시에 하나만 허용한다(record_prompts/quotes와 동일한 규칙).
CREATE UNIQUE INDEX "ix_experiment_programs_code_published" ON "experiment_programs" ("code") WHERE "status" = 'PUBLISHED';
