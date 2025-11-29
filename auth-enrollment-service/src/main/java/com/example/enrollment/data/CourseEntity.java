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

    public CourseEntity() {}

    public CourseEntity(String id, String name, Integer units, Boolean laboratory, String facultyId) {
        this.id = id; this.name = name; this.units = units; this.laboratory = laboratory; this.facultyId = facultyId;
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
}
