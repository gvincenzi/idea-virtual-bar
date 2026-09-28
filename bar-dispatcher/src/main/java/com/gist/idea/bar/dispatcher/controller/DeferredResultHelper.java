package com.gist.idea.bar.dispatcher.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.CompletableFuture;

/**
 * Utility helper to standardize synchronous and asynchronous results into strongly-typed DeferredResult instances.
 */
public final class DeferredResultHelper {

    private DeferredResultHelper() {
        // Prevent instantiation
    }

    /**
     * Wraps an immediate, synchronous response into a DeferredResult.
     *
     * @param response the immediate ResponseEntity
     * @return an already-completed DeferredResult
     */
    public static DeferredResult<ResponseEntity<?>> immediate(ResponseEntity<?> response) {
        DeferredResult<ResponseEntity<?>> deferred = new DeferredResult<>();
        deferred.setResult(response);
        return deferred;
    }

    /**
     * Bridges an asynchronous CompletableFuture into a DeferredResult with a timeout fallback.
     *
     * @param future the asynchronous stage to await
     * @param timeoutMillis timeout in milliseconds
     * @param timeoutPayload the response body to return if the timeout expires
     * @return DeferredResult bound to the future lifecycle
     */
    public static <T> DeferredResult<ResponseEntity<?>> fromFuture(
            CompletableFuture<T> future,
            long timeoutMillis,
            String timeoutPayload) {

        DeferredResult<ResponseEntity<?>> deferred = new DeferredResult<>(timeoutMillis);

        deferred.onTimeout(() -> deferred.setErrorResult(
                ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(timeoutPayload)
        ));

        future.thenAccept(result -> {
            // if result is already a ResponseEntity we don't need to re-encapsulate
            if (result instanceof ResponseEntity<?> responseEntity) {
                deferred.setResult(responseEntity);
            } else {
                deferred.setResult(ResponseEntity.ok(result));
            }
        }).exceptionally(ex -> {
            if (!deferred.isSetOrExpired()) {
                deferred.setErrorResult(
                        ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(ex.getMessage())
                );
            }
            return null;
        });

        return deferred;
    }

}
