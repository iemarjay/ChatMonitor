package io.github.mooeypoo.chatmonitor.words;

import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;

import com.google.common.collect.ImmutableMap;

import io.github.mooeypoo.chatmonitor.configs.ConfigurationException;
import io.github.mooeypoo.chatmonitor.configs.GroupConfigInterface;

@ThreadSafe
public class WordManager {
	private final Logger logger;
	private final Map<String, String> patternToPatternGroup;
	private final Map<String, Set<String>> commandToPatterns;
	private final Map<String, GroupConfigInterface> configGroupData;

	public WordManager(
			Logger logger,
			Map<String, String> patternToPatternGroup,
			Map<String, Set<String>> commandToPatterns,
			Map<String, GroupConfigInterface> configGroupData) {
		this.logger = logger;
		this.patternToPatternGroup = ImmutableMap.copyOf(patternToPatternGroup);
		this.commandToPatterns = ImmutableMap.copyOf(commandToPatterns);
		this.configGroupData = ImmutableMap.copyOf(configGroupData);
	}

	
	/**
	 * Process the given message to see if it triggers a matching word
	 * then grab the details of the word.
	 *
	 * @param chatMessage Given message
	 * @return Details of the matched word from any of the groups, or null if none was matched.
	 */
	public WordAction whatToDoWith(String chatMessage) throws Exception {
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
	 */
	public WordAction whatToDoWith(String commandName, String fullmessage) throws Exception {
		if (!this.commandToPatterns.containsKey(commandName)) {
			return null;
		}

		Set<String> wordListForThisCommand = this.commandToPatterns.get(commandName);
		
		String[] matched = this.getMatchedWord(fullmessage, wordListForThisCommand);
		if (matched == null) {
			return null;
		}

		return this.getWordAction(matched[0], matched[1]);
	}

	private Set<String> getAllWords() {
		return this.patternToPatternGroup.keySet();
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
		String group = this.patternToPatternGroup.get(matchedRule);
		if (group == null) {
			return null; // Todo: throw exception
		}

		try {
			GroupConfigInterface config = getGroupConfigData(group);

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

	private GroupConfigInterface getGroupConfigData(String group) throws ConfigurationException {
		return this.configGroupData.get(group);
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
		return this.commandToPatterns.keySet();
	}
}
