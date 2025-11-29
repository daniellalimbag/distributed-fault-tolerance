package com.example.enrollment.data;

import java.io.Serializable;
import java.util.Objects;

public class EnrollmentId implements Serializable {
    private String studentId;
    private String courseId;

    public EnrollmentId() {}
    public EnrollmentId(String studentId, String courseId) {
        this.studentId = studentId; this.courseId = courseId;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnrollmentId that = (EnrollmentId) o;
        return Objects.equals(studentId, that.studentId) && Objects.equals(courseId, that.courseId);
    }
    @Override public int hashCode() { return Objects.hash(studentId, courseId); }
}
