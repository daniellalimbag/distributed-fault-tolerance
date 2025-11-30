-- Create tables
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS courses (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    units INT NOT NULL,
    laboratory BOOLEAN NOT NULL,
    faculty_id VARCHAR(50) NOT NULL REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS enrollments (
    student_id VARCHAR(50) NOT NULL REFERENCES users(id),
    course_id VARCHAR(50) NOT NULL,
    grade VARCHAR(5),
    PRIMARY KEY (student_id, course_id),
    CONSTRAINT fk_course FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- Insert default users
INSERT INTO users (id, role) VALUES ('student1', 'STUDENT') ON CONFLICT (id) DO NOTHING;
INSERT INTO users (id, role) VALUES ('faculty1', 'FACULTY') ON CONFLICT (id) DO NOTHING;

-- Insert default courses
INSERT INTO courses (id, name, units, laboratory, faculty_id) 
VALUES ('CS101', 'Intro to CS', 3, false, 'faculty1') ON CONFLICT (id) DO NOTHING;

INSERT INTO courses (id, name, units, laboratory, faculty_id) 
VALUES ('CS102', 'Data Structures', 4, true, 'faculty1') ON CONFLICT (id) DO NOTHING;

-- Insert default enrollments
INSERT INTO enrollments (student_id, course_id, grade) 
VALUES ('student1', 'CS101', NULL) ON CONFLICT (student_id, course_id) DO NOTHING;
