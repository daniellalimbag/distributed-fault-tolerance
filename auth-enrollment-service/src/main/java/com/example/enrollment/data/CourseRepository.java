package com.example.enrollment.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseRepository extends JpaRepository<CourseEntity, String> {
    List<CourseEntity> findByFacultyId(String facultyId);
}
