-- Add capacity column to courses and seed values
ALTER TABLE courses ADD COLUMN IF NOT EXISTS capacity INT NOT NULL DEFAULT 30;

-- Seed capacities for default courses if present
UPDATE courses SET capacity = 4 WHERE id = 'CS101';
UPDATE courses SET capacity = 3 WHERE id = 'CS102';
