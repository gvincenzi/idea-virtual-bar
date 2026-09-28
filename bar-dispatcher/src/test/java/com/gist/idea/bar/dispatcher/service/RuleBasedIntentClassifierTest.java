package com.gist.idea.bar.dispatcher.service;

import com.gist.idea.bar.common.model.IntentEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedIntentClassifierTest {

    private final RuleBasedIntentClassifier classifier = new RuleBasedIntentClassifier();

    @ParameterizedTest
    @ValueSource(strings = {
        "Vorrei un cappuccino e un croissant per favore",
        "Prenderei un espresso e una brioche",
        "Coffee and toast, thanks",
        "Un tè caldo e una torta"
    })
    @DisplayName("Should detect compound intents (Drink + Food) in a single sentence (Fan-out)")
    void shouldDetectCompoundIntents(String input) {
        var result = classifier.classify(input);

        assertThat(result.hasIntents()).isTrue();
        assertThat(result.classifierSource()).isEqualTo("RULE_BASED_FALLBACK");
        assertThat(result.detectedIntents())
                .containsExactlyInAnyOrder(IntentEnum.ORDER_DRINK, IntentEnum.ORDER_FOOD);
    }

    @ParameterizedTest
    @CsvSource({
        "'Solo un caffè al volo', ORDER_DRINK",
        "'Vorrei un espresso macchiato', ORDER_DRINK",
        "'Un panino al prosciutto', ORDER_FOOD",
        "'Prendo un croissant', ORDER_FOOD",
        "'A che punto è il mio ordine?', CHECK_STATUS",
        "'Qual è lo stato della comanda?', CHECK_STATUS",
        "'Avvisami quando è tutto pronto', AWAIT_READY",
        "'Dimmi quando ha finito', AWAIT_READY"
    })
    @DisplayName("Should accurately classify single distinct intents")
    void shouldClassifySingleIntents(String input, IntentEnum expectedIntent) {
        var result = classifier.classify(input);

        assertThat(result.hasIntents()).isTrue();
        assertThat(result.detectedIntents()).contains(expectedIntent);
    }

    @Test
    @DisplayName("Should return empty result when input has no recognizable intent")
    void shouldReturnEmptyForUnrecognizedInput() {
        var result = classifier.classify("Oggi c'è un bel sole fuori");

        assertThat(result.hasIntents()).isFalse();
        assertThat(result.detectedIntents()).isEmpty();
    }
}
