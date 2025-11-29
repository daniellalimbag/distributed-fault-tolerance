-- Users (students and faculty)
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    role VARCHAR(16) NOT NULL CHECK (role IN ('STUDENT','FACULTY'))
);

-- Courses (owned by faculty)
CREATE TABLE IF NOT EXISTS courses (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    units INTEGER NOT NULL,
    laboratory BOOLEAN NOT NULL,
    faculty_id VARCHAR(64) NOT NULL REFERENCES users(id)
);

-- Enrollments (student in course) with nullable grade
CREATE TABLE IF NOT EXISTS enrollments (
    student_id VARCHAR(64) NOT NULL REFERENCES users(id),
    course_id VARCHAR(64) NOT NULL REFERENCES courses(id),
    grade VARCHAR(32),
    PRIMARY KEY (student_id, course_id)
);

-- Seed data
INSERT INTO users(id, role) VALUES
    ('f_anything','FACULTY')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users(id, role) VALUES
    ('s_alice','STUDENT'),
    ('s_bob','STUDENT')
ON CONFLICT (id) DO NOTHING;

INSERT INTO courses(id, name, units, laboratory, faculty_id) VALUES
    ('CS101','Intro to CS',3,false,'f_anything'),
    ('CS102','Data Structures',4,true,'f_anything'),
    ('MATH101','Calculus I',3,false,'f_anything')
ON CONFLICT (id) DO NOTHING;
