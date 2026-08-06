-- entry 상세 화면에서 EXPERIMENT_MISSION/EXPERIMENT_REVIEW 기록이 어느 작은 실험 코스에서
-- 만들어졌는지 보여주기 위한 역방향 참조. experiment 도메인이 Entry를 만들 때만 채운다.
ALTER TABLE "entries"
    ADD COLUMN "related_experiment_program_id" uuid NULL
        REFERENCES "user_experiment_programs"("id") ON DELETE SET NULL;

CREATE INDEX "ix_entries_related_experiment_program"
    ON "entries"("related_experiment_program_id")
    WHERE "related_experiment_program_id" IS NOT NULL;
