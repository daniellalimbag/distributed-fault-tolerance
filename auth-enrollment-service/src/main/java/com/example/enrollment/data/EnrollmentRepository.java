package com.example.enrollment.data;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, EnrollmentId> {
    List<EnrollmentEntity> findByStudentId(String studentId);
    List<EnrollmentEntity> findByCourseId(String courseId);
    Optional<EnrollmentEntity> findByStudentIdAndCourseId(String studentId, String courseId);
    long countByCourseId(String courseId);
}
