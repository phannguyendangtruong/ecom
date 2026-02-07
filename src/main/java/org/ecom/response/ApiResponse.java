package org.ecom.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private int code;
    private String traceId;
    private String timestamp;
    private String path;
    private List<String> errors;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> res = new ApiResponse<>();
        res.success = true;
        res.data = data;
        res.code = HttpStatus.OK.value();
        return res;
    }

    public static ApiResponse<Void> okMessage(String message) {
        ApiResponse<Void> res = new ApiResponse<>();
        res.success = true;
        res.code = HttpStatus.OK.value();
        res.message = message;
        return res;
    }

    public static <T> ApiResponse<T> fail(HttpStatus status, String message) {
        return fail(status, message, null, null);
    }

    public static <T> ApiResponse<T> fail(HttpStatus status, String message, String path, List<String> errors) {
        String traceId = MDC.get("traceId");
        ApiResponse<T> res = new ApiResponse<>();
        res.success = false;
        res.code = status.value();
        res.message = message;
        res.traceId = traceId;
        res.timestamp = Instant.now().toString();
        res.path = path;
        res.errors = errors;
        return res;
    }
}
