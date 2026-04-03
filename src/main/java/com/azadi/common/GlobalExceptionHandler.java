package com.azadi.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.stripe.exception.StripeException;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        populateErrorModel(model, 403, "Access Denied",
            "You do not have permission to access this resource.");
        return "error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidation(MethodArgumentNotValidException ex, Model model) {
        var message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        populateErrorModel(model, 400, "Bad Request", message);
        return "error";
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NoSuchElementException ex, Model model) {
        populateErrorModel(model, 404, "Not Found",
            "The page or resource you requested could not be found.");
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        LOG.warn("Invalid request: {}", ex.getMessage());
        populateErrorModel(model, 400, "Invalid Request",
            "The request contained invalid data.");
        return "error";
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleIllegalState(IllegalStateException ex, Model model) {
        LOG.warn("Illegal state: {}", ex.getMessage());
        populateErrorModel(model, 409, "Action Not Allowed",
            "This action cannot be performed at this time.");
        return "error";
    }

    @ExceptionHandler(StripeException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public String handleStripeException(StripeException ex, Model model) {
        LOG.error("Stripe error", ex);
        populateErrorModel(model, 502, "Payment Service Unavailable",
            "Payment service unavailable. Please try again.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(Exception ex, Model model) {
        LOG.error("Unhandled exception", ex);
        populateErrorModel(model, 500, "Something went wrong",
            "An unexpected error has occurred. Please try again later.");
        return "error";
    }

    private static void populateErrorModel(Model model, int status, String error, String message) {
        model.addAttribute("status", status);
        model.addAttribute("error", error);
        model.addAttribute("message", message);
    }
}
