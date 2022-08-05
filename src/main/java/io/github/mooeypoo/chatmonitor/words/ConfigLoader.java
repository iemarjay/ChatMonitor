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
        Map<String, String> patternToPatternGroup = new HashMap<>();
        Map<String, Set<String>> commandToPatterns = new HashMap<>();
        Map<String, GroupConfigInterface> groupConfigData = new HashMap<>();

        for (String patternGroup : groups) {
            try {
                GroupConfigInterface groupConfig = this.configManager.getGroupConfigData(patternGroup);
                groupConfigData.put(patternGroup, groupConfig);

                for (String pattern : groupConfig.words()) {
                    // Save in the pattern map, so we can find the group from the matched pattern
                    patternToPatternGroup.put(pattern, patternGroup);

                    // Check if there are commands that this pattern should be tested against
                    // and add those to the commands map
                    this.collectCommandToPattern(pattern, groupConfig.includeCommands(), commandToPatterns);
                }
            } catch (ConfigurationException e) {
                this.logger.warning("Word group loading defaults. Error in configuration file '" + e.getConfigFileName() + "': " + e.getMessage());
            }
        }

        return new WordConfig(patternToPatternGroup, commandToPatterns, groupConfigData);
    }

    /**
     * Collect the relevant commands per pattern given, based on the group configuration
     * Create a map where command names are keys, and the values are a list of all words
     * that are included in the groups that include this command.
     * @param pattern Given pattern match
     * @param commandsInGroup A set of the commands in the group
     */
    private void collectCommandToPattern(String pattern, Set<String> commandsInGroup, Map<String, Set<String>> commandToPatterns) {
        commandsInGroup.stream()
                .filter(command -> !(command == null || command.isBlank()))
                .forEach(command -> {
                    commandToPatterns
                            .computeIfAbsent(command, s -> new HashSet<>())
                            .add(pattern);
                });
    }
}
