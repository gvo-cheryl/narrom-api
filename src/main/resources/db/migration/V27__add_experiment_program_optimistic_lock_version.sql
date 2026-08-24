-- V26에서 낙관적 잠금을 위해 별도 컬럼 없이 content_version을 그대로 재사용하기로 했으나, 실제로는
-- publish()/archive()처럼 상태만 바뀌는 모든 UPDATE에서도 content_version이 함께 증가해 같은 code를
-- 가진 다른 row와 값이 우연히 겹치면서 (code, content_version) UNIQUE 제약을 위반하는 문제가 있었다
-- (예: 새 리비전 발행 시 자동으로 ARCHIVED되는 이전 발행본의 content_version이 증가하면서 새 리비전의
-- content_version과 충돌). quotes(V25)/record_prompts(V24)와 동일하게 전용 version 컬럼을 둔다.
-- content_version은 다시 순수한 사람이 보는 리비전 번호로 되돌리고, createRevision에서만 명시적으로 +1한다.

ALTER TABLE "experiment_programs" ADD COLUMN "version" bigint NOT NULL DEFAULT 0;
COMMENT ON COLUMN "experiment_programs"."version" IS '낙관적 잠금 전용 컬럼(JPA @Version). content_version과 별개로 모든 UPDATE에서 자동 증가한다.';
