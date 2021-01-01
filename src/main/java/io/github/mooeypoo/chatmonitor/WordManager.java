package io.github.mooeypoo.chatmonitor;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.bukkit.plugin.java.JavaPlugin;

public class WordManager {
	private HashMap<String, String> wordmap = new HashMap<String, String>();
	private HashMap<String, ArrayList<String>> mapWordsInCommands = new HashMap<String, ArrayList<String>>();
	private ArrayList<String> allwords = new ArrayList<String>();
	private ArrayList<String> relevantCommands = new ArrayList<String>();
	private JavaPlugin plugin;
	private ConfigManager configManager;
	
	public WordManager(JavaPlugin plugin) {
		this.plugin = plugin;
		this.configManager = new ConfigManager(Paths.get(this.plugin.getDataFolder().getPath()), "ChatMonitor_wordgroup");
		this.collectWords();
	}
	
	/**
	 * Reload the lists and re-process the groups from the config files.
	 */
	public void reload() {
		// Reset lists
		this.wordmap.clear();
		this.allwords.clear();
		this.mapWordsInCommands.clear();
		this.relevantCommands.clear();
		
		// Refresh all configs 
		this.configManager.reload();

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
	public WordAction processAllWords(String chatMessage) {
		String[] matched = this.getMatchedWord(chatMessage, this.allwords);

		if (matched == null) {
			return null;
		}
		
		return this.getWordAction(matched);
	}

	/**
	 * Process the given command to see if any of its text triggers a matching word
	 * then grab the details of the word.
	 *
	 * @param commandName The name of the command
	 * @param fullmessage Given message
	 * @return Details of the matched word from any of the groups, or null if none was matched.
	 */
	public WordAction processWordsInCommand(String commandName, String fullmessage) {
		ArrayList<String> wordListForThisCommand = this.mapWordsInCommands.get(commandName);

		String[] matched = this.getMatchedWord(fullmessage, wordListForThisCommand);
		if (matched == null) {
			return null;
		}

		return this.getWordAction(matched);
	}
	
	/**
	 * Initialize the lists, collect all words and groups from the config files.
	 */
	private void collectWords() {
		// Go over the groups of words
		Set<String> groups = this.configManager.getGroupNames();

		for (String groupName : groups) {
			GroupConfigInterface groupConfig = this.configManager.getGroupConfigData(groupName);
			// Collect all words from the config group
			for (String word : groupConfig.words()) {
				// Save in full list
				this.allwords.add(word);
				
				// Save in the word map so we can find the group from the matched word
				this.wordmap.put(word, groupName);
				
				// Check if there are commands that this word should be tested against
				// and add those to the commands map
				Set<String> includedCommands = groupConfig.includeCommands();
				if (!includedCommands.isEmpty()) {
					for (String includedCmd : includedCommands) {
						ArrayList<String> listForThisCommand = this.mapWordsInCommands.get(includedCmd);
						if (listForThisCommand == null) {
							// Create a list for this command if one doesn't exist
							listForThisCommand = new ArrayList<String>();
							this.mapWordsInCommands.put(includedCmd, listForThisCommand);

							// Add the command to the list
							this.relevantCommands.add(includedCmd);
						}
						
						// Add the word
						listForThisCommand.add(word);
					}
				}
			}
	
		}
	}

	/**
	 * Produce a WordAction type response from a given word,
	 * based on details of its group and individual config.
	 *
	 * @param matched An array 
	 * @return     Details about the matched word 
	 */
	private WordAction getWordAction(String[] matched) {
		if (matched.length < 2) {
			return null;
		}
		String matchedRule = matched[0];
		String originalWord = matched[1];
		// Find the group this word is in
		String group = this.wordmap.get(matchedRule);
		if (group == null) {
			return null;
		}
		GroupConfigInterface config = this.configManager.getGroupConfigData(group);
//				configs.get(group).getConfig();
		
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
		
	}

	/**
	 * Check whether the test string has any matches from the given word list
	 * and return the matching word.
	 *
	 * @param givenString Given string
	 * @param wordList A word list to test against
	 * @return An array that contains the matching term and original word that was matched,
	 *  or null if none was matched.
	 */
	private String[] getMatchedWord(String givenString, List<String> wordList) {
    	Matcher matcher;
		if (wordList == null || wordList.isEmpty()) {
			return null;
		}
		
		// Transform to owercase for the match test
		String testString = givenString.toLowerCase();
    	// Check if the string has any of the words in the wordlist
		for (String rule : wordList) {
			try {
	    		Pattern pattern = Pattern.compile(rule);
	    		matcher = pattern.matcher(testString);
	    		if (matcher.find()) {
	    			return new String[] { rule, matcher.group() };
	    		}
			} catch (PatternSyntaxException e) {
				this.plugin.getLogger().info("Error: Could not process rule (" + rule + ")");
				return null;
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
	public ArrayList<String> getRelevantCommands() {
		return this.relevantCommands;
	}
}
