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
    private String grade;

    @Column(name = "term_number")
    private Short termNumber;

    @Column(name = "academic_year_range", length = 9)
    private String academicYearRange;

    @Column(name = "completed_at")
    private java.time.Instant completedAt;

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

    public Short getTermNumber() { return termNumber; }
    public void setTermNumber(Short termNumber) { this.termNumber = termNumber; }

    public String getAcademicYearRange() { return academicYearRange; }
    public void setAcademicYearRange(String academicYearRange) { this.academicYearRange = academicYearRange; }

    public java.time.Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(java.time.Instant completedAt) { this.completedAt = completedAt; }
}
