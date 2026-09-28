package com.gist.idea.bar.dispatcher.service;

import com.gist.idea.bar.common.model.IntentEnum;
import com.gist.idea.bar.dispatcher.dto.jev.JevAnswer;
import com.gist.idea.bar.dispatcher.dto.jev.JevQuestion;
import com.gist.idea.bar.dispatcher.dto.jev.JevRequest;
import com.gist.idea.bar.dispatcher.dto.jev.JevResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Primary intent classifier powered by TypeSafe AI's Jev (System One Model).
 * Implements graceful degradation: automatically falls back to RuleBasedIntentClassifier
 * if the external API times out, returns 5xx, or fails.
 */
@Service
@Primary
public class JevIntentClassifier implements IntentClassifier {

    private static final Logger log = LoggerFactory.getLogger(JevIntentClassifier.class);
    private static final String SOURCE = "JEV_AI";
    private static final String YES = "yes";
    private static final String NO = "no";
    private static final double CONFIDENCE_THRESHOLD = 0.65;

    private final RestClient restClient;
    private final String model;
    private final boolean enabled;
    private final Map<String, JevQuestion> questions;
    private final RuleBasedIntentClassifier fallbackClassifier;

    public JevIntentClassifier(
            @Value("${jev.api.url:https://api.typesafe.ai/v1/systemone}") String apiUrl,
            @Value("${jev.api.key:disabled}") String apiKey,
            @Value("${jev.api.model:jev-latest}") String model,
            @Value("${jev.enabled:true}") boolean enabled,
            RuleBasedIntentClassifier fallbackClassifier) {

        this.model = model;
        this.enabled = enabled && !"disabled".equalsIgnoreCase(apiKey);
        this.fallbackClassifier = fallbackClassifier;

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        this.questions = new HashMap<>();
        for (IntentEnum intent : IntentEnum.values()) {
            this.questions.put(intent.getJevKey(), JevQuestion.choice(Map.of(
                YES, intent.getDescription(),
                NO,  "Customer is NOT requesting this"
            )));
        }

        if (!this.enabled) {
            log.warn("Jev AI is disabled or API key not provided. System will default to RuleBasedIntentClassifier.");
        }
    }

    @Override
    public ClassificationResult classify(String userInput) {
        if (!enabled) {
            log.info("Jev AI is offline. Routing directly to fallback classifier.");
            return fallbackClassifier.classify(userInput);
        }

        log.info("[JevClassifier] Submitting speculative fan-out questions to TypeSafe Jev for: '{}'", userInput);
        var request = new JevRequest(model, userInput, questions);
        Set<IntentEnum> matchedIntents = new HashSet<>();

        try {
            JevResponse response = restClient.post()
                    .body(request)
                    .retrieve()
                    .body(JevResponse.class);

            if (response != null && response.results() != null) {
                for (Map.Entry<String, JevAnswer> entry : response.results().entrySet()) {
                    String questionKey = entry.getKey();
                    JevAnswer answer = entry.getValue();

                    double confidence = answer.confidence() != null ? answer.confidence() : 0.0;
                    if (YES.equalsIgnoreCase(answer.choice()) && confidence >= CONFIDENCE_THRESHOLD) {
                        log.info("[JevClassifier] Detected '{}' with confidence {}", questionKey, confidence);
                        IntentEnum.fromJevKey(questionKey).ifPresent(matchedIntents::add);
                    }
                }
                return new ClassificationResult(matchedIntents, SOURCE, !matchedIntents.isEmpty());
            }
        } catch (Exception e) {
            log.error("[JevClassifier] Call to TypeSafe Jev API failed ({}: {}). Triggering Graceful Degradation to RuleBasedClassifier.",
                    e.getClass().getSimpleName(), e.getMessage());
        }

        // Graceful degradation fallback
        return fallbackClassifier.classify(userInput);
    }
}
