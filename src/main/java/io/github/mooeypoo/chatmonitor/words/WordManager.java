package io.github.mooeypoo.chatmonitor.words;

import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nonnull;

import io.github.mooeypoo.chatmonitor.configs.ConfigManager;
import io.github.mooeypoo.chatmonitor.configs.ConfigurationException;

public class WordManager {
    public final Logger logger;
    public final ConfigManager configManager;
    public final WordCollector wordCollector;

    public WordManager(Path filepath, Logger logger) throws ConfigurationException {
        this(filepath, "ChatMonitor_wordgroup", logger);
    }

    // Used for testing
    public WordManager(Path filepath, String prefix, Logger logger) throws ConfigurationException {
        this.logger = logger;

        this.configManager = new ConfigManager(filepath, prefix);
        wordCollector = new WordCollector(this.configManager, logger);
        this.wordCollector.collectWords();
    }

    /**
     * Process the given message to see if it triggers a matching word
     * then grab the details of the word.
     *
     * @param chatMessage Given message
     * @return Details of the matched word from any of the groups, or null if none was matched.
     */
    public WordAction processAllWords(String chatMessage) throws Exception {
        String[] matched = this.getMatchedWord(chatMessage, this.wordCollector.getAllWords(this));

        if (matched == null) {
            return null;
        }

        return this.wordCollector.getWordAction(matched[0], matched[1]);
    }

    /**
     * Process the given command to see if any of its text triggers a matching word
     * then grab the details of the word.
     *
     * @param commandName The name of the command
     * @param fullmessage Given message
     * @return Details of the matched word from any of the groups, or null if none was matched.
     */
    public WordAction processWordsInCommand(String commandName, String fullmessage) throws Exception {
        Set<String> wordListForThisCommand = wordCollector.getWordListForThisCommand(commandName);

        String[] matched = this.getMatchedWord(fullmessage, wordListForThisCommand);
        if (matched == null) {
            return null;
        }

        return this.wordCollector.getWordAction(matched[0], matched[1]);
    }

    /**
     * Check whether the test string has any matches from the given word list
     * and return the matching word.
     *
     * @param givenString Given string
     * @param ruleList    A word list to test against
     * @return An array that contains the matching term and original word that was matched,
     * or null if none was matched.
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
                    return new String[]{rule, matcher.group()};
                }
            } catch (PatternSyntaxException e) {
                throw new Exception("Error: Could not process rule (" + rule + ")");
            }
        }

        return null;
    }

}
