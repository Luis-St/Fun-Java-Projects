package net.luis;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.luis.utils.io.token.TokenReader;
import net.luis.utils.io.token.definition.TokenDefinition;
import net.luis.utils.io.token.rule.TokenRuleEngine;
import net.luis.utils.io.token.rule.actions.GroupingTokenAction;
import net.luis.utils.io.token.rule.actions.SkipTokenAction;
import net.luis.utils.io.token.rule.rules.TokenRule;
import net.luis.utils.io.token.rule.rules.TokenRules;
import net.luis.utils.io.token.tokens.Token;
import net.luis.utils.io.token.tokens.TokenGroup;
import net.luis.utils.logging.*;
import org.apache.logging.log4j.*;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static net.luis.Tokens.*;

/**
 *
 * @author Luis-St
 *
 */

public class Main {
	
	private static final Logger LOGGER;
	
	@Language("Java")
	private static final String TEST_CODE = """
		/*
		 * Simple license header for testing purposes
		 */
		
		package net.luis.test;
		
		import net.luis.utils.io.token.definition.TokenDefinition;
		import net.luis.utils.io.token.Tokens;
		
		/**
		 * This is a test class to demonstrate token reading
		 */
		public class Main {
			// This is an inline comment
		}
		""";
	
	public static void main(String[] args) {
		
		TokenReader reader = new TokenReader(getTokenDefinitions(), getAllowedCharacters(), getSeparators());
		
		LOGGER.info(TokenDefinition.class.getModule().getName());
		
		List<Token> tokens = reader.readTokens(TEST_CODE.replace("\r", ""));
		LOGGER.info("Read {} tokens", tokens.size());
		
		
		TokenRule singleLineComment = TokenRules.boundary(
			SLASH.repeatExactly(2),
			TokenRules.alwaysMatch(),
			NEW_LINE
		);
		TokenRule multiLineComment = TokenRules.boundary(
			TokenRules.sequence(SLASH.repeatBetween(1, 2), ASTERISK),
			TokenRules.alwaysMatch(),
			TokenRules.sequence(ASTERISK, SLASH)
		);
		TokenRule comment = TokenRules.any( // Ugly, should be improved (should be the two rules above)
			Grouped.SINGLE_LINE_COMMENT,
			Grouped.MULTI_LINE_COMMENT
		);
		
		TokenRuleEngine	ruleEngine = new TokenRuleEngine();
		
		// First step: Grouping comments
		ruleEngine.addRule(singleLineComment, new GroupingTokenAction(Grouped.SINGLE_LINE_COMMENT));
		ruleEngine.addRule(multiLineComment, new GroupingTokenAction(Grouped.MULTI_LINE_COMMENT));
		
		tokens = ruleEngine.process(tokens);
		LOGGER.info("After grouping comments: {} tokens", tokens.size());
		
		// Second step: Extracting all comments
		List<TokenGroup> comments = Lists.newArrayList();
		ruleEngine.addRule(comment, new SkipTokenAction(token -> {
			if (token instanceof TokenGroup group) {
				comments.add(group);
				return true;
			}
			return false;
		}));
		
		tokens = ruleEngine.process(tokens);
		LOGGER.info("After extracting comments: {} tokens", tokens.size());
		LOGGER.info("Extracting {} comments from tokens", comments.size());
		comments.forEach(LOGGER::info);
		
	}
	
	private static @NotNull Set<TokenDefinition> getTokenDefinitions() {
		Set<TokenDefinition> tokens = Sets.newHashSet();
		
		List<Field> fields = Lists.newArrayList(Tokens.class.getFields());
		for (Class<?> clazz : Tokens.class.getClasses()) {
			fields.addAll(Lists.newArrayList(clazz.getFields()));
		}
		
		for (Field field : fields) {
			if (TokenDefinition.class.isAssignableFrom(field.getType())) {
				try {
					tokens.add((TokenDefinition) field.get(null));
				} catch (IllegalAccessException e) {
					LOGGER.error("Failed to access token definition: {}", field.getName(), e);
				}
			}
		}
		return tokens;
	}
	
	private static @NotNull Set<Character> getAllowedCharacters() {
		Set<Character> allowedCharacters = Sets.newHashSet();
		
		// Add all alphanumeric characters
		for (char c = 'a'; c <= 'z'; c++) {
			allowedCharacters.add(c);
		}
		for (char c = 'A'; c <= 'Z'; c++) {
			allowedCharacters.add(c);
		}
		
		// Add digits
		for (char c = '0'; c <= '9'; c++) {
			allowedCharacters.add(c);
		}
		
		allowedCharacters.addAll(getSeparators());
		return allowedCharacters;
	}
	
	private static @NotNull Set<Character> getSeparators() {
		Set<Character> separators = Sets.newHashSet();
		
		for (char c : new char[] {
			'.', ',', ':', ';', '=', '+', '-', '*', '/', '%', '&', '|', '!', '?',
			'~', '`', '"', '\'', '(', ')', '[', ']', '{', '}', '<', '>', '@',
			'\\', '$', '^', '_', '#', ' ', '\t', '\n', '\r'
		}) {
			separators.add(c);
		}
		
		return separators;
	}
	
	static {
		System.setProperty("reflection.exceptions.throw", "true");
		LoggingUtils.initialize(LoggerConfiguration.DEFAULT.disableLogging(LoggingType.FILE).addDefaultLogger(LoggingType.CONSOLE, Level.DEBUG));
		LOGGER = LogManager.getLogger(Main.class);
	}
}
