package com.gist.idea.bar.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ItemStateTest {

    @ParameterizedTest
    @EnumSource(ItemState.class)
    @DisplayName("Self-transitions must always be permitted (Idempotency)")
    void selfTransitionsMustBeValid(ItemState state) {
        assertThat(state.canTransitionTo(state)).isTrue();
    }

    @Test
    @DisplayName("ORDERED can transition to any other state")
    void orderedCanTransitionToAnyState() {
        assertThat(ItemState.ORDERED.canTransitionTo(ItemState.PREPARING)).isTrue();
        assertThat(ItemState.ORDERED.canTransitionTo(ItemState.READY)).isTrue();
        assertThat(ItemState.ORDERED.canTransitionTo(ItemState.FAILED)).isTrue();
    }

    @Test
    @DisplayName("READY is terminal and cannot regress to ORDERED or PREPARING")
    void readyCannotRegress() {
        assertThat(ItemState.READY.canTransitionTo(ItemState.ORDERED)).isFalse();
        assertThat(ItemState.READY.canTransitionTo(ItemState.PREPARING)).isFalse();
        assertThat(ItemState.READY.canTransitionTo(ItemState.FAILED)).isFalse();
    }

    @Test
    @DisplayName("FAILED can only transition to READY (Recovery/Retry)")
    void failedCanOnlyRecoverToReady() {
        assertThat(ItemState.FAILED.canTransitionTo(ItemState.READY)).isTrue();
        assertThat(ItemState.FAILED.canTransitionTo(ItemState.ORDERED)).isFalse();
        assertThat(ItemState.FAILED.canTransitionTo(ItemState.PREPARING)).isFalse();
    }
}
