package com.example.enrollment.web.error;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StatusRuntimeException.class)
    public String handleGrpc(StatusRuntimeException ex, Model model) {
        Status.Code code = ex.getStatus().getCode();
        switch (code) {
            case UNAUTHENTICATED:
                return "redirect:/login";
            case UNAVAILABLE:
            case DEADLINE_EXCEEDED:
                model.addAttribute("title", "Service unavailable");
                model.addAttribute("message", "The backend API is currently unavailable. Please try again later.");
                return "service-unavailable";
            default:
                model.addAttribute("title", "Request failed");
                model.addAttribute("message", ex.getStatus().getDescription() != null ? ex.getStatus().getDescription() : "Unexpected error.");
                return "error";
        }
    }
}
