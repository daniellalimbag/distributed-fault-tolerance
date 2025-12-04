-- Add course-level completion fields so courses with zero enrollments can still be marked completed
ALTER TABLE courses ADD COLUMN IF NOT EXISTS term_number SMALLINT;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS academic_year_range VARCHAR(9);
ALTER TABLE courses ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;
