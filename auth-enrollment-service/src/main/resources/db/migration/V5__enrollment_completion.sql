-- Add completion tracking fields to enrollments
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS term_number SMALLINT;
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS academic_year_range VARCHAR(9);
ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

-- Helpful index for querying per course/term/year
CREATE INDEX IF NOT EXISTS idx_enrollments_course_term_year
  ON enrollments(course_id, term_number, academic_year_range);
