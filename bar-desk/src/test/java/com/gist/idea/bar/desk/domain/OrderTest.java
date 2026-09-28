package com.gist.idea.bar.desk.domain;

import com.gist.idea.bar.common.model.ItemState;
import com.gist.idea.bar.common.model.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    @DisplayName("Should NOT regress item state when order intent arrives AFTER ready event (Out-of-Order)")
    void shouldHandleOutOfOrderEventsMonotonically() {
        UUID correlationId = UUID.randomUUID();
        Order order = new Order(correlationId);

        // 1. Ready event arrives first due to network race condition
        order.recordItemReady("cappuccino");

        assertThat(order.getItems().get("cappuccino")).isEqualTo(ItemState.READY);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);

        // 2. Lagging intent event arrives later
        order.recordItemOrdered("cappuccino");

        // Assert: State must NOT regress to ORDERED!
        assertThat(order.getItems().get("cappuccino"))
                .as("State must remain READY and not regress to ORDERED")
                .isEqualTo(ItemState.READY);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
    }

    @Test
    @DisplayName("Order remains IN_PROGRESS until ALL items are READY")
    void shouldRemainInProgressUntilAllItemsAreReady() {
        UUID correlationId = UUID.randomUUID();
        Order order = new Order(correlationId);

        order.recordItemOrdered("cappuccino");
        order.recordItemOrdered("croissant");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);

        // First item becomes ready
        order.recordItemReady("cappuccino");
        assertThat(order.getStatus())
                .as("Order must stay IN_PROGRESS because croissant is still ORDERED")
                .isEqualTo(OrderStatus.IN_PROGRESS);

        // Second item becomes ready
        order.recordItemReady("croissant");
        assertThat(order.getStatus())
                .as("Order must become READY only when both items are ready")
                .isEqualTo(OrderStatus.READY);
    }

    @Test
    @DisplayName("Order transitions to FAILED if any item fails preparation")
    void shouldTransitionToFailedWhenAnItemFails() {
        UUID correlationId = UUID.randomUUID();
        Order order = new Order(correlationId);

        order.recordItemOrdered("cappuccino");
        order.recordItemOrdered("croissant");

        order.recordItemReady("cappuccino");
        order.recordItemFailed("croissant");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(order.getItems().get("croissant")).isEqualTo(ItemState.FAILED);
        assertThat(order.getItems().get("cappuccino")).isEqualTo(ItemState.READY);
    }

    @Test
    @DisplayName("Order self-heals from FAILED to READY when failed item is retried successfully")
    void shouldSelfHealWhenFailedItemIsRetried() {
        UUID correlationId = UUID.randomUUID();
        Order order = new Order(correlationId);

        order.recordItemOrdered("cappuccino");
        order.recordItemFailed("cappuccino");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);

        // Operator retries: drink becomes ready
        order.recordItemReady("cappuccino");

        assertThat(order.getStatus())
                .as("Order must recover to READY after successful retry")
                .isEqualTo(OrderStatus.READY);
        assertThat(order.getItems().get("cappuccino")).isEqualTo(ItemState.READY);
    }

    @Test
    @DisplayName("Empty order starts as RECEIVED")
    void shouldStartAsReceived() {
        UUID correlationId = UUID.randomUUID();
        Order order = new Order(correlationId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(order.getItems()).isEmpty();
    }
}
