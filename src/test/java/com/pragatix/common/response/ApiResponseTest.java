package com.pragatix.common.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void okWithMessageAndDataShouldPopulateSuccessFields() {
        ApiResponse<String> response = ApiResponse.ok("Created", "value");

        assertTrue(response.isSuccess());
        assertEquals("Created", response.getMessage());
        assertNull(response.getError());
        assertEquals("value", response.getData());
    }

    @Test
    void okWithoutMessageShouldUseDefaultSuccessMessage() {
        ApiResponse<String> response = ApiResponse.ok("value");

        assertTrue(response.isSuccess());
        assertEquals("Success", response.getMessage());
        assertNull(response.getError());
        assertEquals("value", response.getData());
    }

    @Test
    void errorShouldReturnFailureResponseWithDetails() {
        ApiResponse<String> response = ApiResponse.error("Bad request", "Validation failed");

        assertFalse(response.isSuccess());
        assertEquals("Bad request", response.getMessage());
        assertEquals("Validation failed", response.getError());
        assertNull(response.getData());
    }

    @Test
    void builderShouldCreateResponseWithConfiguredFields() {
        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(true)
                .message("Built")
                .error("none")
                .data("payload")
                .build();

        assertTrue(response.isSuccess());
        assertEquals("Built", response.getMessage());
        assertEquals("none", response.getError());
        assertEquals("payload", response.getData());
    }
}
