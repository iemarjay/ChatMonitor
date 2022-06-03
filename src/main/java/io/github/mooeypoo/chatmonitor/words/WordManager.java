package io.github.mooeypoo.chatmonitor.words;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nonnull;

import io.github.mooeypoo.chatmonitor.configs.ConfigManager;
import io.github.mooeypoo.chatmonitor.configs.ConfigurationException;
import io.github.mooeypoo.chatmonitor.configs.GroupConfigInterface;

public class WordManager {
	private Logger logger;
	private Map<String, String> wordmap = new HashMap<>();
	private Map<String, Set<String>> mapWordsInCommands = new HashMap<>();
	private List<String> relevantCommands = new ArrayList<>();
	private ConfigManager configManager;

	public WordManager(Path filepath, Logger logger) {
		this(filepath, "ChatMonitor_wordgroup", logger);
	}
	
	// Used for testing
	public WordManager(Path filepath, String prefix, Logger logger) {
		try {
			this.logger = logger;
			this.configManager = new ConfigManager(filepath, prefix);
			this.collectWords();
		} catch (ConfigurationException e) {
			// Todo: bubble exception to caller
			this.logger.warning("Initiation aborted for ChatMonitor. Error in configuration file '" + e.getConfigFileName() + "': " + e.getMessage());

		}
	}
	/**
	 * Reload the lists and re-process the groups from the config files.
	 */
	public void reload() {
		// Reset lists
		this.wordmap.clear();
		this.mapWordsInCommands.clear();
		this.relevantCommands.clear();
		
		// Refresh all configs 
		try {
			this.configManager.reload();
		} catch (ConfigurationException e) {
			logger.warning("Reload loading default config. Error in configuration file '" + e.getConfigFileName() + "': " + e.getMessage());
		}

		// Redo word collection
		this.collectWords();
	}
	
	/**
	 * Process the given message to see if it triggers a matching word
	 * then grab the details of the word.
	 *
	 * @param chatMessage Given message
	 * @return Details of the matched word from any of the groups, or null if none was matched.
	 */
	public WordAction processAllWords(String chatMessage) throws Exception {
		String[] matched = this.getMatchedWord(chatMessage, this.getAllWords());

		if (matched == null) {
			return null;
		}
		
		return this.getWordAction(matched[0], matched[1]);
	}

	/**
	 * Process the given command to see if any of its text triggers a matching word
	 * then grab the details of the word.
	 *
	 * @param commandName The name of the command
	 * @param fullmessage Given message
	 * @return Details of the matched word from any of the groups, or null if none was matched.
	 * @throws Exception 
	 */
	public WordAction processWordsInCommand(String commandName, String fullmessage) throws Exception {
		if (!this.mapWordsInCommands.containsKey(commandName)) {	
			return null;
		}

		Set<String> wordListForThisCommand = this.mapWordsInCommands.get(commandName);
		
		String[] matched = this.getMatchedWord(fullmessage, wordListForThisCommand);
		if (matched == null) {
			return null;
		}

		return this.getWordAction(matched[0], matched[1]);
	}
	
	/**
	 * Initialize the lists, collect all words and groups from the config files.
	 */
	private void collectWords() {
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

	private Set<String> getAllWords() {
		return this.wordmap.keySet();
	}
	
	/**
	 * Collect the relevant commands per word given, based on the group configuration
	 * Create a map where command names are keys, and the values are a list of all words
	 * that are included in the groups that include this command.
	 *
	 * @param word Given word match
	 * @param commandsInGroup A set of the commands in the group
	 */
	private void collectCommandMap(String word, Set<String> commandsInGroup) {
		commandsInGroup.stream()
				.filter(command -> !(command == null || command.isBlank()))
				.forEach(command -> mapWordsInCommands.computeIfAbsent(command, s -> new HashSet<>()).add(word));
	}

	/**
	 * Produce a WordAction type response from a given word,
	 * based on details of its group and individual config.
	 *
	 * @param matchedRule
	 * @param originalWord
	 * @return     Details about the matched word
	 */
	private WordAction getWordAction(String matchedRule, String originalWord) {
		// Find the group this word is in
		String group = this.wordmap.get(matchedRule);
		if (group == null) {
			return null; // Todo: throw exception
		}

		try {
			GroupConfigInterface config = this.configManager.getGroupConfigData(group);

			if (config == null) {
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
		} catch (ConfigurationException e) {
			logger.warning("Aborting generating action. Error in configuration file '" + e.getConfigFileName() + "': " + e.getMessage());
			return null;
		}
	}

	/**
	 * Check whether the test string has any matches from the given word list
	 * and return the matching word.
	 *
	 * @param givenString Given string
	 * @param ruleList A word list to test against
	 * @return An array that contains the matching term and original word that was matched,
	 *  or null if none was matched.
	 * @throws Exception 
	 */
	private String[] getMatchedWord(String givenString, @Nonnull Set<String> ruleList) throws Exception {
		// Transform to lowercase for the match test
		String testString = givenString.toLowerCase();

    	// Check if the string has any of the words in the wordlist
		for (String rule : ruleList) {
			try {
	    		Pattern pattern = Pattern.compile(rule);
	    		Matcher matcher = pattern.matcher(testString);
	    		if (matcher.find()) {
	    			return new String[] { rule, matcher.group() };
	    		}
			} catch (PatternSyntaxException e) {
				throw new Exception("Error: Could not process rule (" + rule + ")");
			}
		}
		
		return null;
	}
	
	/**
	 * Get a list of the commands that the plugin should
	 * know about. This is used to do a naive preliminary
	 * check about whether or not to even process incoming
	 * commands before checking the deeper details, or ignoring
	 * them if they are irrelevant.
	 *
	 * @return List of commands that have words that may match in a string
	 */
	public Set<String> getRelevantCommands() {
		return this.mapWordsInCommands.keySet();
	}
}
