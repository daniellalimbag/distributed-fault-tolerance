package com.example.enrollment.web.controller;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.enrollment.grpc.AuthServiceGrpc;
import com.example.enrollment.grpc.CourseServiceGrpc;
import com.example.enrollment.grpc.CreateCourseRequest;
import com.example.enrollment.grpc.DeleteCourseRequest;
import com.example.enrollment.grpc.EnrollRequest;
import com.example.enrollment.grpc.EnrollmentServiceGrpc;
import com.example.enrollment.grpc.ListCoursesRequest;
import com.example.enrollment.grpc.ListUsersRequest;
import com.example.enrollment.grpc.StudentServiceGrpc;

import jakarta.servlet.http.HttpSession;
import net.devh.boot.grpc.client.inject.GrpcClient;  // ✅ FIXED

@Controller
public class CourseController {

    @GrpcClient("course")
    private CourseServiceGrpc.CourseServiceBlockingStub courseStub;

    @GrpcClient("authEnroll")
    private AuthServiceGrpc.AuthServiceBlockingStub authStub;

    @GrpcClient("authEnroll")
    private EnrollmentServiceGrpc.EnrollmentServiceBlockingStub enrollmentStub;

    @GrpcClient("authEnroll")
    private StudentServiceGrpc.StudentServiceBlockingStub studentStub;

    @GetMapping("/courses")
    public String listCourses(Model model, HttpSession session) {
        Object role = session.getAttribute("role");
        if (role != null && "FACULTY".equals(role.toString())) 
            return "redirect:/dashboard";

        var response = courseStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .listCourses(ListCoursesRequest.newBuilder().build());

        var allCourses = response.getCoursesList();
        java.util.List<com.example.enrollment.grpc.Course> courses = new java.util.ArrayList<>();
        java.util.Map<String, Integer> enrolledCounts = new java.util.HashMap<>();

        for (var c : allCourses) {
            try {
                var compResp = enrollmentStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                        .isCourseCompleted(
                                com.example.enrollment.grpc.IsCourseCompletedRequest.newBuilder()
                                        .setCourseId(c.getId())
                                        .build()
                        );
                if (compResp.getCompleted()) continue;
            } catch (Exception e) {
                // ignore
            }

            courses.add(c);

            try {
                var countResp = enrollmentStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                        .getEnrollmentCount(
                                com.example.enrollment.grpc.GetEnrollmentCountRequest.newBuilder()
                                        .setCourseId(c.getId())
                                        .build()
                        );
                enrolledCounts.put(c.getId(), countResp.getCount());
            } catch (Exception e) {
                enrolledCounts.put(c.getId(), 0);
            }
        }

        model.addAttribute("courses", courses);
        model.addAttribute("enrolledCounts", enrolledCounts);
        model.addAttribute("isStudent", role != null && "STUDENT".equals(role.toString()));
        model.addAttribute("isAdmin", role != null && "ADMIN".equals(role.toString()));
        model.addAttribute("role", role);


        return "courses";
    }

    @GetMapping("/admin/courses/edit")
    public String showEditCourseForm(@RequestParam String courseId,
                                    Model model,
                                    HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("role"))) {
            return "redirect:/dashboard";
        }
        
        System.out.println("HIT EDIT CONTROLLER, courseId = " + courseId);

        // Get all courses and find the one we want (since we don't have a GetCourse RPC)
        var response = courseStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .listCourses(ListCoursesRequest.newBuilder().build());

        com.example.enrollment.grpc.Course course =
        response.getCoursesList().stream()
                .filter(c -> c.getId().replaceAll("\\s+", "").equalsIgnoreCase(courseId.replaceAll("\\s+", "")))
                .findFirst()
                .orElse(null);


        if (course == null) {
            model.addAttribute("error", "Course not found");
            return "redirect:/courses";
        }

        // Get faculty list (same as create)
        var userResponse = authStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .listUsers(ListUsersRequest.newBuilder().build());

        var facultyList = userResponse.getUsersList().stream()
                .filter(u -> "FACULTY".equals(u.getRole()))
                .toList();

        model.addAttribute("course", course);
        model.addAttribute("facultyList", facultyList);

        return "admin-edit-course";
    }


    @GetMapping("/admin/courses/create")
    public String showCreateCourseForm(Model model, HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("role")))
            return "redirect:/dashboard";

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
                               @RequestParam(defaultValue = "false") boolean laboratory,
                               @RequestParam String facultyId,
                               @RequestParam(defaultValue = "30") Integer capacity,
                               HttpSession session,
                               Model model) {

        if (!"ADMIN".equals(session.getAttribute("role"))) {
            model.addAttribute("error", "Access denied. Only ADMIN can create courses.");
            return "admin-create-course";
        }

        try {
            courseStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .createCourse(
                            CreateCourseRequest.newBuilder()
                                    .setId(id)
                                    .setName(name)
                                    .setUnits(units)
                                    .setLaboratory(laboratory)
                                    .setFacultyId(facultyId)
                                    .setCapacity(capacity)
                                    .build()
                    );

            model.addAttribute("message", "Course created successfully!");
            return "redirect:/dashboard";

        } catch (Exception e) {
            model.addAttribute("error", "Failed to create course: " + e.getMessage());
            return "admin-create-course";
        }
    }


    @PostMapping("/enroll")
    public String enroll(@RequestParam String courseId,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {

        String studentId = String.valueOf(session.getAttribute("username"));
        Object role = session.getAttribute("role");

        if ("FACULTY".equals(role)) return "redirect:/dashboard";
        if (studentId == null) return "redirect:/login";

        try {
            enrollmentStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .enroll(
                            EnrollRequest.newBuilder()
                                    .setStudentId(studentId)
                                    .setCourseId(courseId)
                                    .build()
                    );
            redirectAttributes.addFlashAttribute("successMessage", "Course enrolled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Enrollment failed: " + e.getMessage());
        }

        return "redirect:/courses";
    }

    @PostMapping("/admin/courses/delete")
    public String deleteCourse(@RequestParam String courseId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        if (!"ADMIN".equals(session.getAttribute("role"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied.");
            return "redirect:/dashboard";
        }

        try {
            var resp = courseStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .deleteCourse(
                            DeleteCourseRequest.newBuilder()
                                    .setCourseId(courseId)
                                    .build()
                    );

            if (resp.getSuccess()) {
                redirectAttributes.addFlashAttribute("successMessage", "Course removed successfully.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", resp.getMessage());
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting course: " + e.getMessage());
        }

        return "redirect:/courses";
    }

    @PostMapping("/admin/courses/edit")
    public String updateCourse(@RequestParam String id,
                            @RequestParam String name,
                            @RequestParam Integer units,
                            @RequestParam(defaultValue = "false") boolean laboratory,
                            @RequestParam String facultyId,
                            @RequestParam Integer capacity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {

        if (!"ADMIN".equals(session.getAttribute("role"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied. Only ADMIN can edit courses.");
            return "redirect:/dashboard";
        }

        try {
            var req = com.example.enrollment.grpc.UpdateCourseRequest.newBuilder()
                    .setId(id)
                    .setName(name)
                    .setUnits(units)
                    .setLaboratory(laboratory)
                    .setFacultyId(facultyId)
                    .setCapacity(capacity)
                    .build();

            var resp = courseStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                    .updateCourse(req);

            if (resp.getSuccess()) {
                redirectAttributes.addFlashAttribute("successMessage", "Course updated successfully!");
                return "redirect:/courses";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", resp.getMessage());
                return "redirect:/admin/courses/edit?courseId=" + id;
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update course: " + e.getMessage());
            return "redirect:/admin/courses/edit?courseId=" + id;
        }
    }


    // (other methods remain unchanged)
}
