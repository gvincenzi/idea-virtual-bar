package com.gist.idea.bar.dispatcher.service;

import com.gist.idea.bar.common.model.IntentEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Deterministic, offline-capable intent classifier.
 * Serves as an immediate fallback when external AI services are unavailable,
 * and as an offline development mode without external API keys.
 */
@Component
public class RuleBasedIntentClassifier implements IntentClassifier {

    private static final Logger log = LoggerFactory.getLogger(RuleBasedIntentClassifier.class);
    private static final String SOURCE = "RULE_BASED_FALLBACK";

    private final Map<IntentEnum, Pattern> intentPatterns = Map.of(
        IntentEnum.ORDER_DRINK, Pattern.compile("(?i).*(caff[eè]|espresso|cappuccino|t[eè]|tea|coffee|water|acqua|drink|succo|latte).*"),
        IntentEnum.ORDER_FOOD, Pattern.compile("(?i).*(croissant|brioche|panino|toast|sandwich|cake|torta|food|muffin|snack).*"),
        IntentEnum.CHECK_STATUS, Pattern.compile("(?i).*(status|stato|punto|progress|pronto|ready\\?|waiting).*"),
        IntentEnum.AWAIT_READY, Pattern.compile("(?i).*(avvisami|notify|wait|aspett.*|alert.*|dimmi quando).*")
    );

    @Override
    public ClassificationResult classify(String userInput) {
        log.info("[RuleBasedClassifier] Evaluating input locally: '{}'", userInput);
        Set<IntentEnum> matched = new HashSet<>();

        for (Map.Entry<IntentEnum, Pattern> entry : intentPatterns.entrySet()) {
            if (entry.getValue().matcher(userInput).matches()) {
                matched.add(entry.getKey());
            }
        }

        log.info("[RuleBasedClassifier] Matched intents: {}", matched);
        return new ClassificationResult(matched, SOURCE, !matched.isEmpty());
    }
}
