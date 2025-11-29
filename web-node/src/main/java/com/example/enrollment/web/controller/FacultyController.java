package com.example.enrollment.web.controller;

import com.example.enrollment.grpc.CourseServiceGrpc;
import com.example.enrollment.grpc.CreateCourseRequest;
import com.example.enrollment.grpc.EnrollmentServiceGrpc;
import com.example.enrollment.grpc.ListFacultyCoursesRequest;
import com.example.enrollment.grpc.ListStudentsInCourseRequest;
import com.example.enrollment.grpc.UploadGradeRequest;
import com.example.enrollment.grpc.GradesServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/faculty")
public class FacultyController {

    @GrpcClient("api")
    private CourseServiceGrpc.CourseServiceBlockingStub courseStub;

    @GrpcClient("api")
    private EnrollmentServiceGrpc.EnrollmentServiceBlockingStub enrollmentStub;

    @GrpcClient("api")
    private GradesServiceGrpc.GradesServiceBlockingStub gradesStub;

    private boolean isFaculty(HttpSession session) {
        Object role = session.getAttribute("role");
        return role != null && "FACULTY".equals(role.toString());
    }

    @GetMapping("/courses")
    public String myCourses(HttpSession session, Model model) {
        if (!isFaculty(session)) return "redirect:/dashboard";
        String facultyId = String.valueOf(session.getAttribute("username"));
        var resp = courseStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .listFacultyCourses(ListFacultyCoursesRequest.newBuilder().setFacultyId(facultyId).build());
        model.addAttribute("courses", resp.getCoursesList());
        return "faculty-courses";
    }

    @GetMapping("/courses/create")
    public String createCourseForm(HttpSession session) {
        if (!isFaculty(session)) return "redirect:/dashboard";
        return "faculty-create-course";
    }

    @PostMapping("/courses/create")
    public String createCourse(HttpSession session,
                               @RequestParam String id,
                               @RequestParam String name,
                               @RequestParam int units,
                               @RequestParam(defaultValue = "false") boolean laboratory,
                               Model model) {
        if (!isFaculty(session)) return "redirect:/dashboard";
        courseStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .createCourse(CreateCourseRequest.newBuilder()
                        .setId(id)
                        .setName(name)
                        .setUnits(units)
                        .setLaboratory(laboratory)
                        .build());
        return "redirect:/faculty/courses";
    }

    @GetMapping("/courses/{courseId}")
    public String courseStudents(@PathVariable String courseId,
                                 @RequestParam(name = "editStudent", required = false) String editStudent,
                                 HttpSession session,
                                 Model model) {
        if (!isFaculty(session)) return "redirect:/dashboard";
        var resp = enrollmentStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .listStudentsInCourse(ListStudentsInCourseRequest.newBuilder().setCourseId(courseId).build());
        model.addAttribute("courseId", courseId);
        model.addAttribute("entries", resp.getEntriesList());
        model.addAttribute("editStudent", editStudent);
        return "faculty-course-students";
    }

    @GetMapping("/courses/{courseId}/grade/{studentId}")
    public String editGradeForm(@PathVariable String courseId,
                                @PathVariable String studentId) {
        return "redirect:/faculty/courses/" + courseId + "?editStudent=" + studentId;
    }

    @PostMapping("/courses/{courseId}/grade/{studentId}")
    public String editGrade(@PathVariable String courseId,
                            @PathVariable String studentId,
                            @RequestParam String grade,
                            HttpSession session) {
        if (!isFaculty(session)) return "redirect:/dashboard";
        gradesStub.withDeadlineAfter(3, TimeUnit.SECONDS)
                .uploadGrade(UploadGradeRequest.newBuilder()
                        .setStudentId(studentId)
                        .setCourseId(courseId)
                        .setGrade(grade)
                        .build());
        return "redirect:/faculty/courses/" + courseId;
    }
}
