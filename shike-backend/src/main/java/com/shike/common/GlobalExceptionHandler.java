package com.shike.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResultDTO<?> handleBizException(BizException e) {
        log.warn("Business Exception: {}", e.getMessage());
        return ResultDTO.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResultDTO<?> handleValidationException(Exception e) {
        String msg = "Validation failed";
        if (e instanceof MethodArgumentNotValidException ex) {
            msg = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        } else if (e instanceof BindException ex) {
            msg = ex.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        }
        log.warn("Validation Exception: {}", msg);
        return ResultDTO.error(400, msg);
    }

    @ExceptionHandler(Exception.class)
    public ResultDTO<?> handleException(Exception e) {
        log.error("Internal Server Error: ", e);
        return ResultDTO.error(500, "Internal Server Error: " + e.getMessage());
    }
}
