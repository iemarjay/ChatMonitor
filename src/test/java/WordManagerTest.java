import static org.junit.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import io.github.mooeypoo.chatmonitor.configs.ConfigManager;
import io.github.mooeypoo.chatmonitor.configs.ConfigurationException;
import io.github.mooeypoo.chatmonitor.words.ConfigLoader;
import io.github.mooeypoo.chatmonitor.words.WordAction;
import io.github.mooeypoo.chatmonitor.words.WordConfig;
import io.github.mooeypoo.chatmonitor.words.WordManager;

public class WordManagerTest {
	@Test
	public void testValidAndEmptyWordMatches() throws Exception {
		WordManager wordManager = newWordManager(Paths.get("src", "test", "resources", "validrules"));
		WordAction action = null;

		action = wordManager.whatToDoWith("there is somebadw0rd in here.");
		assertEquals(action.getOriginalWord(), "badw0rd");
		assertEquals(action.getMatchedRule(), "badw[0o]rd");

		action = wordManager.whatToDoWith("this is a w0rd in a sentence");
		assertEquals(action.getOriginalWord(), "w0rd");
		assertEquals(action.getMatchedRule(), "\\bw0rd\\b");

		action = wordManager.whatToDoWith("There are no matches here.");
		assertNull(action);
	}

	@Test
	public void testEmptyLists() throws Exception {
		WordManager wordManager = newWordManager(Paths.get("src", "test", "resources", "emptylist"));
		WordAction action = null;
		
		action = wordManager.whatToDoWith("this should be skipped gracefully since there are no words in this list");
		assertNull(action);
	}

	@Test
	public void testInvalidRules() throws ConfigurationException {
		WordManager wordManager = newWordManager(Paths.get("src", "test", "resources", "invalidrule"));

		try {
			wordManager.whatToDoWith("There is a validword match here from a problematic invalid rule");
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error: Could not process rule ((invalid)");
		}
	}
	
	@Test
	public void testMatchesInCommands() throws Exception {
		WordManager wordManager = newWordManager(Paths.get("src", "test", "resources", "commands"));
		WordAction action = null;

		action = wordManager.whatToDoWith("tell", "This badw0rd should be found.");
		assertNotNull(action);
		assertEquals(action.getOriginalWord(), "badw0rd");
		assertEquals(action.getMatchedRule(), "badw[0o]rd");

		action = wordManager.whatToDoWith("me", "This badw0rd should not be found because the group doesn't specify that command.");
		assertNull(action);

		action = wordManager.whatToDoWith("tell", "There is no match in the words even though the command itself is evaluated.");
		assertNull(action);

		action = wordManager.whatToDoWith("me", "the word justme should match for that command");
		assertNotNull(action);
		action = wordManager.whatToDoWith("tell", "the word justme should NOT match for that command");
		assertNull(action);
		
		// Check that wordManager.getRelevantCommands() has all commands that have words in them
		List<String> expectedRelevantCommands = asList("me", "tell");
		assertTrue(
			wordManager.getRelevantCommands().containsAll(expectedRelevantCommands) &&
			expectedRelevantCommands.containsAll(wordManager.getRelevantCommands())
		);
	}

	@NotNull
	private WordManager newWordManager(Path path) throws ConfigurationException {
		final ConfigManager configManager = new ConfigManager(path, "test_");
		final ConfigLoader configLoader = new ConfigLoader(configManager, Logger.getLogger("chat_monitor"));
		WordConfig wordConfig = configLoader.collectWords();
		return new WordManager(
				Logger.getLogger("chat_monitor"), wordConfig.patternToPatternGroup(), wordConfig.commandToPatterns(),
				wordConfig.configGroupData());
	}
}

