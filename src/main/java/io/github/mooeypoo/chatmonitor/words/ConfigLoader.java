package io.github.mooeypoo.chatmonitor.words;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import io.github.mooeypoo.chatmonitor.configs.ConfigManager;
import io.github.mooeypoo.chatmonitor.configs.ConfigurationException;
import io.github.mooeypoo.chatmonitor.configs.GroupConfigInterface;

public class ConfigLoader {
    private final ConfigManager configManager;
    private final Logger logger;

    public ConfigLoader(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    /**
     * Initialize the lists, collect all words and groups from the config files.
     * @return
     */
    public WordConfig collectWords() {
        // Go over the groups of words
        Set<String> groups = this.configManager.getGroupNames();
        Map<String, String> wordMap = new HashMap<>();
        Map<String, Set<String>> wordsInCommandsMap = new HashMap<>();

        for (String groupName : groups) {
            try {
                GroupConfigInterface groupConfig = this.configManager.getGroupConfigData(groupName);

                for (String word : groupConfig.words()) {
                    // Save in the word map, so we can find the group from the matched word
                    wordMap.put(word, groupName);

                    // Check if there are commands that this word should be tested against
                    // and add those to the commands map
                    this.collectCommandMap(word, groupConfig.includeCommands(), wordsInCommandsMap);
                }
            } catch (ConfigurationException e) {
                this.logger.warning("Word group loading defaults. Error in configuration file '" + e.getConfigFileName() + "': " + e.getMessage());
            }
        }

        return new WordConfig(wordMap, wordsInCommandsMap);
    }

    /**
     * Collect the relevant commands per word given, based on the group configuration
     * Create a map where command names are keys, and the values are a list of all words
     * that are included in the groups that include this command.
     * @param word Given word match
     * @param commandsInGroup A set of the commands in the group
     * @param wordsInCommandsMap
     */
    private void collectCommandMap(String word, Set<String> commandsInGroup, Map<String, Set<String>> wordsInCommandsMap) {
        commandsInGroup.stream()
                .filter(command -> !(command == null || command.isBlank()))
                .forEach(command -> {
                    wordsInCommandsMap.computeIfAbsent(command, s -> new HashSet<>()).add(word);
                });
    }
}
