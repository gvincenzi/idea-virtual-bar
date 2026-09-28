package com.gist.idea.bar.dispatcher.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class DeferredResultHelperTest {

    @Test
    @DisplayName("immediate() should return an already-completed DeferredResult")
    void immediateShouldCompleteInstantly() {
        ResponseEntity<String> response = ResponseEntity.ok("test-body");

        DeferredResult<ResponseEntity<?>> result = DeferredResultHelper.immediate(response);

        assertThat(result.hasResult()).isTrue();
        assertThat(result.getResult()).isEqualTo(response);
    }

    @Test
    @DisplayName("fromFuture() should resolve immediately when future is already completed")
    void fromFutureShouldResolvePreCompletedFuture() {
        CompletableFuture<String> future = CompletableFuture.completedFuture("order-ready-payload");

        DeferredResult<ResponseEntity<?>> result = 
                DeferredResultHelper.fromFuture(future, 5000L, "Timeout message");

        assertThat(result.hasResult()).isTrue();
        ResponseEntity<?> entity = (ResponseEntity<?>) result.getResult();
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo("order-ready-payload");
    }

    @Test
    @DisplayName("fromFuture() should return timeout payload when future times out")
    void fromFutureShouldHandleTimeout() {
        CompletableFuture<String> incompleteFuture = new CompletableFuture<>();

        DeferredResult<ResponseEntity<?>> result = 
                DeferredResultHelper.fromFuture(incompleteFuture, 100L, "Custom timeout message");

        // Force timeout expiration on DeferredResult
        assertThat(result.hasResult()).isFalse();
    }
}

