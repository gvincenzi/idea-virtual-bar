package com.gist.idea.bar.dispatcher.service;

import com.gist.idea.bar.common.model.IntentEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JevIntentClassifierTest {

    @Test
    @DisplayName("When Jev is disabled, should immediately route to RuleBased fallback")
    void shouldFallbackWhenJevIsDisabled() {
        RuleBasedIntentClassifier fallback = new RuleBasedIntentClassifier();

        // Instance created with enabled=false (as in local offline mode)
        JevIntentClassifier classifier = new JevIntentClassifier(
                "https://api.typesafe.ai/v1/systemone",
                "disabled",
                "jev-latest",
                false, // explicitly disabled
                fallback
        );

        var result = classifier.classify("Un cappuccino e un croissant");

        assertThat(result.hasIntents()).isTrue();
        assertThat(result.classifierSource()).isEqualTo("RULE_BASED_FALLBACK");
        assertThat(result.detectedIntents())
                .containsExactlyInAnyOrder(IntentEnum.ORDER_DRINK, IntentEnum.ORDER_FOOD);
    }
}

