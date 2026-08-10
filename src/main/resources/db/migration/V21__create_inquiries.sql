CREATE TABLE "inquiries" (
    "id" uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    "member_id" uuid NOT NULL REFERENCES "members" ("id") ON DELETE CASCADE,
    "content" text NOT NULL,
    "created_at" timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE "inquiries" IS '마이페이지 문의하기로 접수된 사용자 문의';
COMMENT ON COLUMN "inquiries"."id" IS '문의 ID';
COMMENT ON COLUMN "inquiries"."member_id" IS '문의한 회원';
COMMENT ON COLUMN "inquiries"."content" IS '문의 내용';
COMMENT ON COLUMN "inquiries"."created_at" IS '문의 접수 시각';
CREATE INDEX "ix_inquiries_created" ON "inquiries" ("created_at");
