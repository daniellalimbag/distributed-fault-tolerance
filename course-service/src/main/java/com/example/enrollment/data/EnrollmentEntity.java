package com.example.enrollment.data;

import jakarta.persistence.*;

@Entity
@IdClass(EnrollmentId.class)
@Table(name = "enrollments")
public class EnrollmentEntity {
    @Id
    @Column(name = "student_id", length = 64)
    private String studentId;

    @Id
    @Column(name = "course_id", length = 64)
    private String courseId;

    @Column(name = "grade", length = 32)
    private String grade; // nullable

    public EnrollmentEntity() {}

    public EnrollmentEntity(String studentId, String courseId, String grade) {
        this.studentId = studentId; this.courseId = courseId; this.grade = grade;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
}
