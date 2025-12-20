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
import net.luis.utils.io.token.definition.TokenDefinition;
import net.luis.utils.io.token.grammar.Grammar;
import net.luis.utils.io.token.tokens.*;
import net.luis.utils.io.token.type.TokenType;
import net.luis.utils.logging.*;
import org.apache.logging.log4j.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;

import static net.luis.Tokens.*;

/**
 * Test file for demonstrating numeric literal token parsing.
 * Run this class to see how different numeric literals are tokenized and parsed.
 *
 * @author Luis-St
 */
public class NumericTokenTest {

	private static final Logger LOGGER;

	public static final String RESET = "\u001B[0m";
	public static final String GREEN = "\u001B[32m";
	public static final String YELLOW = "\u001B[33m";
	public static final String CYAN = "\u001B[36m";
	public static final String MAGENTA = "\u001B[35m";
	public static final String BG_GRAY = "\u001B[100m";

	public static void main(String[] args) {
		Grammar grammar = Grammar.builder(builder -> {
			NumericGrammarDefinition definition = new NumericGrammarDefinition(builder);
			definition.addNumericRules();
		});

		TokenReader reader = new TokenReader(
			getTokenDefinitions(),
			getAllowedCharacters(),
			getSeparatorCharacters(),
			NumericTokenTest::classifyToken
		);

		String testCode = """
			// Decimal integers
			int a = 0;
			int b = 42;
			int c = 123;
			long d = 9223372036854775807L;
			int e = 1_000_000;

			// Hexadecimal integers
			int hex1 = 0x0;
			int hex2 = 0x1A;
			int hex3 = 0XDEADBEEF;
			long hex4 = 0xFFFFL;
			int hex5 = 0xDE_AD_BE_EF;

			// Binary integers
			int bin1 = 0b0;
			int bin2 = 0b1010;
			int bin3 = 0B1111;
			long bin4 = 0b1111_0000L;

			// Octal integers
			int oct1 = 00;
			int oct2 = 0777;
			int oct3 = 012;

			// Decimal floating-point
			double f1 = 0.0;
			double f2 = 3.14;
			double f3 = .5;
			double f4 = 123.;
			float f5 = 3.14f;
			double f6 = 1.23D;
			double f7 = 3.14_15_92;

			// Decimal floating-point with exponent
			double e1 = 1e10;
			double e2 = 1.23e10;
			double e3 = .5e-2;
			double e4 = 123.e5;
			float e5 = 1e10f;
			double e6 = 1.0e1_000;

			// Hexadecimal floating-point
			double hf1 = 0x1p0;
			double hf2 = 0x1.2p3;
			double hf3 = 0X1.FP-2;
			double hf4 = 0x1.91eb851eb851fp+6;
			double hf5 = 0x.8p0;
			float hf6 = 0x1.2p3f;
			""";

		LOGGER.info("{}=== Testing Numeric Token Parsing ==={}\n", CYAN, RESET);

		List<Token> rawTokens = reader.readTokens(testCode);
		List<Token> parsedTokens = grammar.parse(rawTokens);

		LOGGER.info("{}Raw Tokens: {}{}", YELLOW, rawTokens.size(), RESET);
		LOGGER.info("{}Parsed Tokens: {}{}", YELLOW, parsedTokens.size(), RESET);

		LOGGER.info("\n{}=== Parsed Content ==={}\n", CYAN, RESET);
		printTokens(parsedTokens);

		LOGGER.info("\n{}=== Numeric Literals Found ==={}\n", CYAN, RESET);
		printNumericLiterals(parsedTokens);

		LOGGER.info("\n{}=== Statistics ==={}\n", CYAN, RESET);
		printStatistics(parsedTokens);
	}

	private static void printTokens(@NotNull List<Token> tokens) {
		StringBuilder sb = new StringBuilder();
		for (Token token : tokens) {
			if (token instanceof TokenGroup) {
				sb.append(GREEN).append(token.value()).append(RESET);
			} else if (token instanceof ShadowToken) {
				sb.append(BG_GRAY).append(token.value()).append(RESET);
			} else {
				sb.append(token.value());
			}
		}
		System.out.println(sb);
	}

	private static void printNumericLiterals(@NotNull List<Token> tokens) {
		int count = 0;
		for (Token token : tokens) {
			if (token instanceof TokenGroup group) {
				String value = group.value();
				String type = categorizeNumeric(value);
				count++;
				LOGGER.info("{}{}{}. {}{}  - {}{}{}",
					YELLOW, String.format("%3d", count), MAGENTA,
					value, " ".repeat(Math.max(0, 25 - value.length())),
					CYAN, type, RESET);
			}
		}
	}

	private static @NotNull String categorizeNumeric(@NotNull String value) {
		if (value.matches(NumericTokens.Patterns.HEX_FLOAT)) {
			return "Hexadecimal Float";
		} else if (value.matches(NumericTokens.Patterns.DECIMAL_FLOAT_EXP)) {
			return "Decimal Float (with exponent)";
		} else if (value.matches(NumericTokens.Patterns.DECIMAL_FLOAT)) {
			return "Decimal Float";
		} else if (value.matches(NumericTokens.Patterns.HEX_INT)) {
			return "Hexadecimal Integer";
		} else if (value.matches(NumericTokens.Patterns.BINARY_INT)) {
			return "Binary Integer";
		} else if (value.matches(NumericTokens.Patterns.OCTAL_INT)) {
			return "Octal Integer";
		} else if (value.matches(NumericTokens.Patterns.DECIMAL_INT)) {
			return "Decimal Integer";
		}
		return "Unknown";
	}

	private static void printStatistics(@NotNull List<Token> tokens) {
		int hexFloats = 0;
		int decimalFloatsExp = 0;
		int decimalFloats = 0;
		int hexInts = 0;
		int binaryInts = 0;
		int octalInts = 0;
		int decimalInts = 0;

		for (Token token : tokens) {
			if (token instanceof TokenGroup group) {
				String value = group.value();
				if (value.matches(NumericTokens.Patterns.HEX_FLOAT)) {
					hexFloats++;
				} else if (value.matches(NumericTokens.Patterns.DECIMAL_FLOAT_EXP)) {
					decimalFloatsExp++;
				} else if (value.matches(NumericTokens.Patterns.DECIMAL_FLOAT)) {
					decimalFloats++;
				} else if (value.matches(NumericTokens.Patterns.HEX_INT)) {
					hexInts++;
				} else if (value.matches(NumericTokens.Patterns.BINARY_INT)) {
					binaryInts++;
				} else if (value.matches(NumericTokens.Patterns.OCTAL_INT)) {
					octalInts++;
				} else if (value.matches(NumericTokens.Patterns.DECIMAL_INT)) {
					decimalInts++;
				}
			}
		}

		LOGGER.info("{}Hexadecimal Floats:{} {}", CYAN, RESET, hexFloats);
		LOGGER.info("{}Decimal Floats (with exp):{} {}", CYAN, RESET, decimalFloatsExp);
		LOGGER.info("{}Decimal Floats:{} {}", CYAN, RESET, decimalFloats);
		LOGGER.info("{}Hexadecimal Integers:{} {}", CYAN, RESET, hexInts);
		LOGGER.info("{}Binary Integers:{} {}", CYAN, RESET, binaryInts);
		LOGGER.info("{}Octal Integers:{} {}", CYAN, RESET, octalInts);
		LOGGER.info("{}Decimal Integers:{} {}", CYAN, RESET, decimalInts);
		LOGGER.info("{}Total Numeric Literals:{} {}", YELLOW, RESET,
			hexFloats + decimalFloatsExp + decimalFloats + hexInts + binaryInts + octalInts + decimalInts);
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
		allowedCharacters.add('_');
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
		separatorCharacters.add(' ');
		separatorCharacters.add('\n');
		separatorCharacters.add('\t');
		return separatorCharacters;
	}

	private static @NotNull Set<TokenType> classifyToken(@NotNull Token token) {
		return new HashSet<>();
	}

	static {
		System.setProperty("reflection.exceptions.throw", "true");
		LoggingUtils.initialize(LoggerConfiguration.DEFAULT.disableLogging(LoggingType.FILE).addDefaultLogger(LoggingType.CONSOLE, Level.INFO));
		LOGGER = LogManager.getLogger(NumericTokenTest.class);
	}
}
