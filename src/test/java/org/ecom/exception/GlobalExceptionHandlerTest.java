package org.ecom.exception;

import org.ecom.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDataIntegrityReturnsConflict() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/user/create");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("unique constraint violation");

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrity(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Duplicate entry detected", response.getBody().getMessage());
    }

    @Test
    void handleTypeMismatchReturnsBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/user/abc");
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", null, new IllegalArgumentException("bad id")
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleTypeMismatch(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid value for parameter: id", response.getBody().getMessage());
    }

    @Test
    void handleMethodNotSupportedReturns405() {
        MockHttpServletRequest request = new MockHttpServletRequest("TRACE", "/auth/login");
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("TRACE");

        ResponseEntity<ApiResponse<Void>> response = handler.handleMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals("Method not allowed", response.getBody().getMessage());
    }
}
