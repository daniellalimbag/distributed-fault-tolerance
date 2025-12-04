package com.example.enrollment.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses")
public class CourseEntity {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer units;

    @Column(nullable = false)
    private Boolean laboratory;

    @Column(name = "faculty_id", length = 64, nullable = false)
    private String facultyId;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "term_number")
    private Short termNumber;

    @Column(name = "academic_year_range", length = 9)
    private String academicYearRange;

    @Column(name = "completed_at")
    private java.time.Instant completedAt;

    public CourseEntity() {}

    public CourseEntity(String id, String name, Integer units, Boolean laboratory, String facultyId) {
        this.id = id; this.name = name; this.units = units; this.laboratory = laboratory; this.facultyId = facultyId;
    }

    public CourseEntity(String id, String name, Integer units, Boolean laboratory, String facultyId, Integer capacity) {
        this.id = id; this.name = name; this.units = units; this.laboratory = laboratory; this.facultyId = facultyId; this.capacity = capacity;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getUnits() { return units; }
    public void setUnits(Integer units) { this.units = units; }
    public Boolean getLaboratory() { return laboratory; }
    public void setLaboratory(Boolean laboratory) { this.laboratory = laboratory; }
    public String getFacultyId() { return facultyId; }
    public void setFacultyId(String facultyId) { this.facultyId = facultyId; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Short getTermNumber() { return termNumber; }
    public void setTermNumber(Short termNumber) { this.termNumber = termNumber; }

    public String getAcademicYearRange() { return academicYearRange; }
    public void setAcademicYearRange(String academicYearRange) { this.academicYearRange = academicYearRange; }

    public java.time.Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(java.time.Instant completedAt) { this.completedAt = completedAt; }
}
