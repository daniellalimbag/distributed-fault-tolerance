package com.example.enrollment.web.controller;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.enrollment.grpc.CourseServiceGrpc;
import com.example.enrollment.grpc.CreateCourseRequest;
import com.example.enrollment.grpc.CreateCourseResponse;
import com.example.enrollment.grpc.AuthServiceGrpc;
import com.example.enrollment.grpc.ListUsersRequest;
import com.example.enrollment.grpc.EnrollRequest;
import com.example.enrollment.grpc.EnrollmentServiceGrpc;
import com.example.enrollment.grpc.ListCoursesRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

@Controller
public class CourseController {

    @GrpcClient("course")
    private CourseServiceGrpc.CourseServiceBlockingStub courseStub;

    @GrpcClient("authEnroll")
    private AuthServiceGrpc.AuthServiceBlockingStub authStub;

    @GrpcClient("authEnroll")
    private EnrollmentServiceGrpc.EnrollmentServiceBlockingStub enrollmentStub;

    @GrpcClient("authEnroll")
    private com.example.enrollment.grpc.StudentServiceGrpc.StudentServiceBlockingStub studentStub;

    @GetMapping("/courses")
    public String listCourses(Model model, HttpSession session) {
        Object role = session.getAttribute("role");
        if (role != null && "FACULTY".equals(role.toString())) return "redirect:/dashboard";
        var response = courseStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .listCourses(ListCoursesRequest.newBuilder().build());

        var courses = response.getCoursesList();
        java.util.Map<String, Integer> enrolledCounts = new java.util.HashMap<>();
        for (var c : courses) {
            try {
                var countResp = enrollmentStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                        .getEnrollmentCount(com.example.enrollment.grpc.GetEnrollmentCountRequest
                                .newBuilder().setCourseId(c.getId()).build());
                enrolledCounts.put(c.getId(), countResp.getCount());
            } catch (Exception e) {
                enrolledCounts.put(c.getId(), 0);
            }
        }
        model.addAttribute("courses", courses);
        model.addAttribute("enrolledCounts", enrolledCounts);
        model.addAttribute("isStudent", role != null && "STUDENT".equals(role.toString()));
        return "courses";
    }

    @GetMapping("/admin/courses/create")
    public String showCreateCourseForm(Model model, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return "redirect:/dashboard";
        }

        var userResponse = authStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .listUsers(ListUsersRequest.newBuilder().build());

        var facultyList = userResponse.getUsersList().stream()
                .filter(u -> "FACULTY".equals(u.getRole()))
                .toList();

        model.addAttribute("facultyList", facultyList);
        return "admin-create-course";
    }

    @PostMapping("/admin/courses/create")
    public String createCourse(@RequestParam String id,
                            @RequestParam String name,
                            @RequestParam Integer units,
                            @RequestParam(required = false, defaultValue = "false") boolean laboratory,
                            @RequestParam String facultyId,
                            @RequestParam(defaultValue = "30") Integer capacity,
                            HttpSession session,
                            Model model) {
        try {
            if (!"ADMIN".equals(session.getAttribute("role"))) {
                model.addAttribute("error", "Access denied. Only ADMIN can create courses.");
                return "admin-create-course";
            }

            var resp = courseStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .createCourse(CreateCourseRequest.newBuilder()
                            .setId(id)
                            .setName(name)
                            .setUnits(units)
                            .setLaboratory(laboratory)
                            .setFacultyId(facultyId)
                            .setCapacity(capacity)
                            .build());

            model.addAttribute("message", "Course created successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create course: " + e.getMessage());
            return "admin-create-course";
        }
    }


    @PostMapping("/enroll")
    public String enroll(@RequestParam String courseId, HttpSession session, RedirectAttributes redirectAttributes) {
        String studentId = String.valueOf(session.getAttribute("username"));
        Object role = session.getAttribute("role");
        if (role != null && "FACULTY".equals(role.toString())) return "redirect:/dashboard";
        if (studentId == null) return "redirect:/login";
        try {
            enrollmentStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .enroll(EnrollRequest.newBuilder().setStudentId(studentId).setCourseId(courseId).build());
            redirectAttributes.addFlashAttribute("successMessage", "Course enrolled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Enrollment failed: " + e.getMessage());
        }
        return "redirect:/courses";
    }

    @GetMapping("/admin/courses/capacity")
    public String editCapacityPage(Model model, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("role"))) return "redirect:/dashboard";
        var response = courseStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .listCourses(ListCoursesRequest.newBuilder().build());
        model.addAttribute("courses", response.getCoursesList());
        return "admin-edit-capacity";
    }

    @PostMapping("/admin/courses/capacity")
    public String updateCapacity(@RequestParam String id,
                                 @RequestParam Integer capacity,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!"ADMIN".equals(session.getAttribute("role"))) return "redirect:/dashboard";
        try {
            com.example.enrollment.grpc.UpdateCourseCapacityRequest req = com.example.enrollment.grpc.UpdateCourseCapacityRequest
                    .newBuilder().setId(id).setCapacity(capacity).build();
            courseStub.withDeadlineAfter(3, TimeUnit.SECONDS).updateCourseCapacity(req);
            redirectAttributes.addFlashAttribute("successMessage", "Capacity updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed: " + e.getMessage());
        }
        return "redirect:/admin/courses/capacity";
    }

    @GetMapping("/admin/courses/complete")
    public String completeCoursePage(Model model, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("role"))) return "redirect:/dashboard";
        var response = courseStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .listCourses(ListCoursesRequest.newBuilder().build());
        model.addAttribute("courses", response.getCoursesList());
        return "admin-complete-course";
    }

    @PostMapping("/admin/courses/complete")
    public String completeCourse(@RequestParam String courseId,
                                 @RequestParam Integer termNumber,
                                 @RequestParam String academicYearRange,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!"ADMIN".equals(session.getAttribute("role"))) return "redirect:/dashboard";
        try {
            var req = com.example.enrollment.grpc.CompleteCourseRequest.newBuilder()
                    .setCourseId(courseId)
                    .setTermNumber(termNumber)
                    .setAcademicYearRange(academicYearRange)
                    .build();
            var resp = enrollmentStub.withDeadlineAfter(5, TimeUnit.SECONDS).completeCourse(req);
            redirectAttributes.addFlashAttribute("successMessage", "Completed course. Affected enrollments: " + resp.getAffected());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to complete course: " + e.getMessage());
        }
        return "redirect:/admin/courses/complete";
    }

    @GetMapping("/student/history")
    public String studentHistory(Model model, HttpSession session) {
        Object role = session.getAttribute("role");
        String studentId = String.valueOf(session.getAttribute("username"));
        if (role == null || !"STUDENT".equals(role.toString())) return "redirect:/dashboard";
        try {
            var resp = studentStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                    .getStudentHistory(com.example.enrollment.grpc.GetStudentHistoryRequest.newBuilder()
                            .setStudentId(studentId)
                            .build());
            model.addAttribute("entries", resp.getEntriesList());
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("entries", java.util.Collections.emptyList());
        }
        return "student-history";
    }

    @PostMapping("/grades/drop")
    public String dropCourse(@RequestParam String courseId, HttpSession session, RedirectAttributes redirectAttributes) {
        String studentId = String.valueOf(session.getAttribute("username"));
        Object role = session.getAttribute("role");
        if (role == null || !"STUDENT".equals(role.toString())) return "redirect:/dashboard";

        if(studentId == null){
            return "redirect:/login";
        }

    try {
        var response = enrollmentStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .drop(EnrollRequest.newBuilder()
                        .setStudentId(studentId)
                        .setCourseId(courseId)
                        .build());

        if (response.getSuccess()) {
            redirectAttributes.addFlashAttribute("successMessage", "Course dropped successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to drop course: " + response.getMessage());
        }

    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Error: " + e.getMessage());
    }

    return "redirect:/grades";
    }
}
