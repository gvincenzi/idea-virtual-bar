package com.gist.idea.bar.dispatcher.service;

import com.gist.idea.bar.common.model.IntentEnum;
import com.gist.idea.bar.dispatcher.dto.jev.JevAnswer;
import com.gist.idea.bar.dispatcher.dto.jev.JevQuestion;
import com.gist.idea.bar.dispatcher.dto.jev.JevRequest;
import com.gist.idea.bar.dispatcher.dto.jev.JevResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Multi-intent classifier using Jev speculative fan-out.
 * Evaluates multiple independent questions in a single request to detect compound intents
 * (e.g. "I want a coffee and a croissant" -> [ORDER_DRINK, ORDER_FOOD]).
 */
@Service
public class IntentClassifierService {

    private static final Logger log = LoggerFactory.getLogger(IntentClassifierService.class);
    private static final double CONFIDENCE_THRESHOLD = 0.65;

    private final RestClient restClient;
    private final String model;
    private final Map<String, JevQuestion> questions;

    public IntentClassifierService(
            @Value("${jev.api.url:https://api.typesafe.ai/v1/systemone}") String apiUrl,
            @Value("${jev.api.key}") String apiKey,
            @Value("${jev.api.model:jev-latest}") String model) {

        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        // 4 independent choice questions evaluated concurrently by Jev in 1 single call
        this.questions = new HashMap<>();
        for (IntentEnum intent : IntentEnum.values()) {
            this.questions.put(intent.getJevKey(), JevQuestion.choice(Map.of(
                "yes", intent.getDescription(),
                "no",  "Customer is NOT requesting this"
            )));
        }

    }

    /**
     * Set of detected business intents from the customer's input.
     */
    public record MultiIntentResult(
        Set<IntentEnum> detectedIntents,
        boolean hasIntents
    ) {}

    /**
     * Evaluates customer input against all 4 questions in a single Jev call.
     *
     * @param userInput free-form customer text (e.g. "I'd like a cappuccino and a croissant")
     * @return MultiIntentResult containing all positive intents
     */
    public MultiIntentResult classify(String userInput) {
        var request = new JevRequest(model, userInput, questions);
        Set<IntentEnum> matchedIntents = new HashSet<>();

        log.info("Submitting fan-out questions to TypeSafe Jev for: '{}'", userInput);

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
                    if ("yes".equalsIgnoreCase(answer.choice()) && confidence >= CONFIDENCE_THRESHOLD) {
                        log.info("Detected intent '{}' with confidence {}", questionKey, confidence);
                        IntentEnum.fromJevKey(questionKey).ifPresent(matchedIntents::add);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error calling TypeSafe Jev API: {}", e.getMessage(), e);
        }

        return new MultiIntentResult(matchedIntents, !matchedIntents.isEmpty());
    }
}
