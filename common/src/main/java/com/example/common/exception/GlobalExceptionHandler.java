package com.example.common.exception;

import com.example.common.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return toResponse(ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return toResponse(DataIntegrityViolations.toBusinessException(ex));
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiResponse<Void>> handleTransactionSystem(TransactionSystemException ex) {
        DataIntegrityViolationException dive = DataIntegrityViolations.findDataIntegrity(ex);
        if (dive != null) {
            return handleDataIntegrity(dive);
        }
        String traceId = ApiResponse.newTraceId();
        log.error("[common-exception] transaction system exception. traceId={}", traceId, ex);
        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.fail(code, code.getDefaultMessage(), traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        return invalidInput(bindingErrors(ex));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBindException(BindException ex) {
        return invalidInput(bindingErrors(ex));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
            ConstraintViolationException ex) {
        Map<String, String> errors =
                ex.getConstraintViolations().stream()
                        .collect(
                                Collectors.toMap(
                                        v -> v.getPropertyPath().toString(),
                                        ConstraintViolation::getMessage,
                                        (a, b) -> a,
                                        LinkedHashMap::new));
        return invalidInput(errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        return invalidInput("요청 본문을 읽을 수 없습니다");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        return invalidInput("필수 파라미터입니다: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return invalidInput("값 형식이 올바르지 않습니다 (" + ex.getName() + ")");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandled(Exception ex) {
        String traceId = ApiResponse.newTraceId();
        log.error("[common-exception] unhandled exception. traceId={}", traceId, ex);
        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.fail(code, code.getDefaultMessage(), traceId));
    }

    private static Map<String, String> bindingErrors(BindException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return errors;
    }

    private static ResponseEntity<ApiResponse<Map<String, String>>> invalidInput(Map<String, String> errors) {
        ErrorCode code = ErrorCode.INVALID_INPUT;
        String traceId = ApiResponse.newTraceId();
        return ResponseEntity.status(code.getHttpStatus())
                .body(ApiResponse.failWithData(code, code.getDefaultMessage(), errors, traceId));
    }

    private static ResponseEntity<ApiResponse<Void>> invalidInput(String message) {
        ErrorCode code = ErrorCode.INVALID_INPUT;
        String traceId = ApiResponse.newTraceId();
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.fail(code, message, traceId));
    }

    private static ResponseEntity<ApiResponse<Void>> toResponse(BusinessException ex) {
        String traceId = ApiResponse.newTraceId();
        return ResponseEntity.status(ex.getStatus())
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage(), traceId));
    }
}
