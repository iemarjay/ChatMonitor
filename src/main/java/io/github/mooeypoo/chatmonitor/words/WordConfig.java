package io.github.mooeypoo.chatmonitor.words;

import java.util.Map;
import java.util.Set;

import io.github.mooeypoo.chatmonitor.configs.GroupConfigInterface;

public record WordConfig(Map<String, String> wordMap,
                         Map<String, Set<String>> wordsInCommandsMap, Map<String, GroupConfigInterface> configGroupData) {
}
