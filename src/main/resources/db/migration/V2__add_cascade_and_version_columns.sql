-- ============================================================
-- Migration V2: Add optimistic locking version columns
--               and fix orphaned-record cascade deletes
-- ============================================================

-- 1. Add version columns for optimistic locking
-- (Hibernate will use these for @Version fields)

ALTER TABLE fee_record
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE course
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 2. Fix orphaned FeeRecord when a Course is deleted
--    Drop the existing FK (no cascade) and re-add with SET NULL
--    so fee history is preserved but course reference is nulled.

ALTER TABLE fee_record
    DROP FOREIGN KEY IF EXISTS fk_fee_record_course;

ALTER TABLE fee_record
    ADD CONSTRAINT fk_fee_record_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE SET NULL;

-- 3. Fix orphaned FeeTransaction when a FeeRecord is deleted
--    Cascade delete transactions when the parent record is removed.

ALTER TABLE fee_transaction
    DROP FOREIGN KEY IF EXISTS fk_fee_transaction_record;

ALTER TABLE fee_transaction
    ADD CONSTRAINT fk_fee_transaction_record
    FOREIGN KEY (fee_record_id) REFERENCES fee_record(id)
    ON DELETE CASCADE;

-- 4. Fix orphaned Attendance when a Course is deleted

ALTER TABLE attendance
    DROP FOREIGN KEY IF EXISTS fk_attendance_course;

ALTER TABLE attendance
    ADD CONSTRAINT fk_attendance_course
    FOREIGN KEY (course_id) REFERENCES course(id)
    ON DELETE CASCADE;
