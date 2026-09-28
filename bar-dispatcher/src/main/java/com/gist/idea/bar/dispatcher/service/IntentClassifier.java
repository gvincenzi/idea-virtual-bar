package com.gist.idea.bar.dispatcher.service;

import com.gist.idea.bar.common.model.IntentEnum;

import java.util.Set;

/**
 * Strategy interface for intent classification.
 * Decouples the Spike gateway from any specific AI provider or deterministic engine.
 */
public interface IntentClassifier {

    /**
     * Strongly-typed classification outcome.
     *
     * @param detectedIntents the set of positively identified business intents
     * @param classifierSource name of the engine that produced the verdict (e.g. "JEV_AI", "RULE_BASED_FALLBACK")
     * @param hasIntents whether at least one actionable intent was recognized
     */
    record ClassificationResult(
        Set<IntentEnum> detectedIntents,
        String classifierSource,
        boolean hasIntents
    ) {
        public static ClassificationResult empty(String source) {
            return new ClassificationResult(Set.of(), source, false);
        }
    }

    /**
     * Evaluates a natural language user statement into one or more business intents.
     *
     * @param userInput free-text input
     * @return ClassificationResult containing detected intents
     */
    ClassificationResult classify(String userInput);
}
