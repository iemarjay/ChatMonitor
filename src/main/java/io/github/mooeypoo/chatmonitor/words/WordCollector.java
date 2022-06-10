package io.github.mooeypoo.chatmonitor.words;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import io.github.mooeypoo.chatmonitor.configs.ConfigManager;
import io.github.mooeypoo.chatmonitor.configs.ConfigurationException;
import io.github.mooeypoo.chatmonitor.configs.GroupConfigInterface;

public class WordCollector {


    public final Logger logger;
    public final Map<String, String> wordmap = new HashMap<>();
    public final Map<String, Set<String>> mapWordsInCommands = new HashMap<>();
    public final ConfigManager configManager;

    public WordCollector(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    /**
     * Initialize the lists, collect all words and groups from the config files.
     */
    void collectWords() {
        // Go over the groups of words
        Set<String> groups = this.configManager.getGroupNames();

        for (String groupName : groups) {
            try {
                GroupConfigInterface groupConfig = this.configManager.getGroupConfigData(groupName);

                for (String word : groupConfig.words()) {
                    // Save in the word map, so we can find the group from the matched word
                    this.wordmap.put(word, groupName);

                    // Check if there are commands that this word should be tested against
                    // and add those to the commands map
                    this.collectCommandMap(word, groupConfig.includeCommands());
                }
            } catch (ConfigurationException e) {
                logger.warning("Word group loading defaults. Error in configuration file '" + e.getConfigFileName() + "': " + e.getMessage());
            }
        }
    }

    /**
     * Collect the relevant commands per word given, based on the group configuration
     * Create a map where command names are keys, and the values are a list of all words
     * that are included in the groups that include this command.
     *  @param word            Given word match
     * @param commandsInGroup A set of the commands in the group
     */
     private void collectCommandMap(String word, Set<String> commandsInGroup) {
        commandsInGroup.stream()
                .filter(command -> !(command == null || command.isBlank()))
                .forEach(command -> mapWordsInCommands.computeIfAbsent(command, s -> new HashSet<>()).add(word));
    }

    /**
     * Reload the lists and re-process the groups from the config files.
     */
    public void reload() {
        // Reset lists
        wordmap.clear();
        mapWordsInCommands.clear();

        // Refresh all configs
        try {
            configManager.reload();
        } catch (ConfigurationException e) {
            logger.warning("Reload loading default config. Error in configuration file '" + e.getConfigFileName() + "': " + e.getMessage());
        }

        // Redo word collection
        collectWords();
    }

    Set<String> getAllWords(WordManager wordManager) {
        return wordmap.keySet();
    }

    /**
     * Get a list of the commands that the plugin should
     * know about. This is used to do a naive preliminary
     * check about whether or not to even process incoming
     * commands before checking the deeper details, or ignoring
     * them if they are irrelevant.
     *
     * @return List of commands that have words that may match in a string
     * @param wordManager
     */
    public Set<String> getRelevantCommands(WordManager wordManager) {
        return mapWordsInCommands.keySet();
    }

    /**
     * Produce a WordAction type response from a given word,
     * based on details of its group and individual config.
     *
     * @param matchedRule
     * @param originalWord
     * @return Details about the matched word
     */
    WordAction getWordAction(String matchedRule, String originalWord) {
        // Find the group this word is in
        String group = this.wordmap.get(matchedRule);
        if (group == null) {
            return null; // Todo: throw exception
        }

        GroupConfigInterface config;

        try {
            config = this.configManager.getGroupConfigData(group);

            if (config == null) {
                return null;
            }
        } catch (ConfigurationException e) {
            this.logger.warning("Aborting generating action. Error in configuration file '" + e.getConfigFileName() + "': " + e.getMessage());
            return null;
        }

        // From the group, get the message and commands
        return new WordAction(
                matchedRule,
                originalWord,
                config.message(),
                config.preventSend(),
                config.broadcast(),
                config.runCommands(),
                group
        );
    }

    Set<String> getWordListForThisCommand(String commandName) {
        if (!this.mapWordsInCommands.containsKey(commandName)) {
            return new HashSet<>();
        }

        return this.mapWordsInCommands.get(commandName);
    }
}
