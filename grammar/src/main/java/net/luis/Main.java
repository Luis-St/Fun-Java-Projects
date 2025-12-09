/*
 * Fun-Java-Projects
 * Copyright (C) 2025 Luis Staudt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.luis;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.luis.utils.io.token.TokenReader;
import net.luis.utils.io.token.actions.TokenActions;
import net.luis.utils.io.token.definition.TokenDefinition;
import net.luis.utils.io.token.grammar.Grammar;
import net.luis.utils.io.token.rules.TokenRule;
import net.luis.utils.io.token.rules.TokenRules;
import net.luis.utils.io.token.tokens.*;
import net.luis.utils.io.token.type.TokenType;
import net.luis.utils.logging.*;
import org.apache.logging.log4j.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static net.luis.Tokens.*;

/**
 *
 * @author Luis-St
 *
 */

public class Main {
	
	private static final Logger LOGGER;
	
	//region ANSI color codes
	public static final String BLACK = "\u001B[30m";
	public static final String GRAY = "\u001B[90m";
	public static final String RED = "\u001B[31m";
	public static final String GREEN = "\u001B[32m";
	public static final String YELLOW = "\u001B[33m";
	public static final String BLUE = "\u001B[34m";
	public static final String MAGENTA = "\u001B[35m";
	public static final String CYAN = "\u001B[36m";
	public static final String WHITE = "\u001B[37m";
	
	public static final String BG_BLACK = "\u001B[40m";
	public static final String BG_GRAY = "\u001B[100m";
	public static final String BG_RED = "\u001B[41m";
	public static final String BG_GREEN = "\u001B[42m";
	public static final String BG_YELLOW = "\u001B[43m";
	public static final String BG_BLUE = "\u001B[44m";
	public static final String BG_MAGENTA = "\u001B[45m";
	public static final String BG_CYAN = "\u001B[46m";
	public static final String BG_WHITE = "\u001B[47m";
	
	public static final String RESET = "\u001B[0m";
	//endregion
	
	public static void main(String[] args) throws Exception {
		Grammar grammar = Grammar.builder(builder -> {
			GrammarDefinition definition = new GrammarDefinition(builder);
			definition.defineRules();
			definition.addPreRules();
			definition.addRules();
		});
		
		TokenReader reader = new TokenReader(
			getTokenDefinitions(),
			getAllowedCharacters(),
			getSeparatorCharacters(),
			Main::classifyToken
		);
		
		String fileContent = Files.readString(Path.of("./src/main/java/net/luis/ComprehensiveSyntaxTest.java"));
		List<Token> rawTokens = reader.readTokens(fileContent);
		List<Token> parsedTokens = grammar.parse(rawTokens);
		
		LOGGER.info("Raw Tokens: {}", rawTokens.size());
		LOGGER.info("Parsed Tokens: {}", parsedTokens.size());
		LOGGER.info("Content:\n{}", parsedTokens.stream().map(Main::stringifyToken).collect(Collectors.joining()));
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
		Set<Character> allowedCharacters = new HashSet<>();
		
		for (char c = 'a'; c <= 'z'; c++) {
			allowedCharacters.add(c);
		}
		for (char c = 'A'; c <= 'Z'; c++) {
			allowedCharacters.add(c);
		}
		for (char c = '0'; c <= '9'; c++) {
			allowedCharacters.add(c);
		}
		allowedCharacters.add('#');
		allowedCharacters.add('$');
		allowedCharacters.add('~');
		allowedCharacters.add('^');
		return allowedCharacters;
	}
	
	private static @NotNull Set<Character> getSeparatorCharacters() {
		Set<Character> separatorCharacters = new HashSet<>();
		separatorCharacters.add('.');
		separatorCharacters.add(',');
		separatorCharacters.add(':');
		separatorCharacters.add(';');
		separatorCharacters.add('=');
		separatorCharacters.add('+');
		separatorCharacters.add('-');
		separatorCharacters.add('*');
		separatorCharacters.add('/');
		separatorCharacters.add('%');
		separatorCharacters.add('&');
		separatorCharacters.add('|');
		separatorCharacters.add('!');
		separatorCharacters.add('?');
		separatorCharacters.add('"');
		separatorCharacters.add('\'');
		separatorCharacters.add('(');
		separatorCharacters.add(')');
		separatorCharacters.add('[');
		separatorCharacters.add(']');
		separatorCharacters.add('{');
		separatorCharacters.add('}');
		separatorCharacters.add('<');
		separatorCharacters.add('>');
		separatorCharacters.add('@');
		separatorCharacters.add('\\');
		separatorCharacters.add('_');
		separatorCharacters.add(' ');
		separatorCharacters.add('\n');
		separatorCharacters.add('\t');
		return separatorCharacters;
	}
	
	private static @NotNull Set<TokenType> classifyToken(@NotNull Token token) {
		return new HashSet<>();
	}
	
	private static @NotNull String stringifyToken(@NotNull Token token) {
		if (token instanceof TokenGroup(List<Token> tokens)) {
			StringBuilder sb = new StringBuilder();
			for (Token childToken : tokens) {
				if (childToken instanceof ShadowToken) {
					sb.append(BG_GRAY).append(childToken.value()).append(RESET);
				} else {
					sb.append(GREEN).append(childToken.value()).append(RESET);
				}
			}
			return sb.toString();
		} else if (token instanceof ShadowToken) {
			return BG_GRAY + token.value() + RESET;
		}
		return token.value();
	}
	
	static {
		System.setProperty("reflection.exceptions.throw", "true");
		LoggingUtils.initialize(LoggerConfiguration.DEFAULT.disableLogging(LoggingType.FILE).addDefaultLogger(LoggingType.CONSOLE, Level.DEBUG));
		LOGGER = LogManager.getLogger(Main.class);
	}
}
