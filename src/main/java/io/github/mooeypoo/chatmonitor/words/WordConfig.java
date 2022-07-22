package io.github.mooeypoo.chatmonitor.words;

import java.util.Map;
import java.util.Set;

public record WordConfig(Map<String, String> wordMap,
                         Map<String, Set<String>> wordsInCommandsMap) {
}
