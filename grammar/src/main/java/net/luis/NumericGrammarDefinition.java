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

import net.luis.utils.io.token.actions.TokenActions;
import net.luis.utils.io.token.actions.core.GroupingMode;
import net.luis.utils.io.token.grammar.GrammarBuilder;
import net.luis.utils.io.token.rules.TokenRules;
import org.jetbrains.annotations.NotNull;

/**
 * Grammar definition for Java numeric literals according to the official Java Language Specification.
 * Handles both pattern-based matching (for literals without separators) and sequence-based matching
 * (for literals containing '.' or '_' which are separator characters in the TokenReader).
 *
 * @author Luis-St
 */
public record NumericGrammarDefinition(@NotNull GrammarBuilder builder) {

	/**
	 * Adds all numeric literal rules to the grammar.
	 * Rules are ordered by specificity to ensure correct matching.
	 */
	public void addNumericRules() {
		this.defineNumericPatterns();

		// Floating-point rules (highest priority due to specificity)
		this.addHexFloatWithDotRules();
		this.addHexFloatNoDotRule();
		this.addDecimalFloatWithExponentAndDotRules();
		this.addDecimalFloatWithExponentNoDotRule();
		this.addDecimalFloatNoDotRules();

		// Integer rules with underscores
		this.addOctalIntWithUnderscoreRule();
		this.addHexIntWithUnderscoreRule();
		this.addBinaryIntWithUnderscoreRule();
		this.addDecimalIntWithUnderscoreRule();

		// Simple integer rules (pattern-based, no separators)
		this.addHexIntRule();
		this.addBinaryIntRule();
		this.addOctalIntRule();
		this.addDecimalIntRule();
	}

	/**
	 * Defines reusable patterns for numeric components.
	 */
	private void defineNumericPatterns() {
		// Basic digit patterns
		this.builder.defineRule("Digits", TokenRules.pattern("[0-9]+"));
		this.builder.defineRule("HexDigits", TokenRules.pattern("[0-9a-fA-F]+"));
		this.builder.defineRule("OctalDigits", TokenRules.pattern("[0-7]+"));
		this.builder.defineRule("BinaryDigits", TokenRules.pattern("[01]+"));

		// Digits with attached suffix (when suffix is not separated)
		this.builder.defineRule("DigitsWithFloatSuffix", TokenRules.pattern("[0-9]+[fFdD]"));
		this.builder.defineRule("DigitsWithLongSuffix", TokenRules.pattern("[0-9]+[lL]"));
		this.builder.defineRule("HexDigitsWithFloatSuffix", TokenRules.pattern("[0-9a-fA-F]+[fFdD]"));
		this.builder.defineRule("HexDigitsWithLongSuffix", TokenRules.pattern("[0-9a-fA-F]+[lL]"));
		this.builder.defineRule("BinaryDigitsWithLongSuffix", TokenRules.pattern("[01]+[lL]"));

		// Hex digits with exponent attached (for hex floats)
		this.builder.defineRule("HexDigitsWithHexExponent", TokenRules.pattern("[0-9a-fA-F]+[pP][+-]?[0-9]+"));
		this.builder.defineRule("HexDigitsWithHexExponentAndSuffix", TokenRules.pattern("[0-9a-fA-F]+[pP][+-]?[0-9]+[fFdD]"));

		// Decimal digits with exponent attached (for decimal floats)
		this.builder.defineRule("DigitsWithExponent", TokenRules.pattern("[0-9]+[eE][+-]?[0-9]+"));
		this.builder.defineRule("DigitsWithExponentAndSuffix", TokenRules.pattern("[0-9]+[eE][+-]?[0-9]+[fFdD]"));
		this.builder.defineRule("DigitsWithExponentStart", TokenRules.pattern("[0-9]+[eE][+-]?[0-9]+"));

		// Hex digits with partial exponent (for handling underscores in exponent)
		this.builder.defineRule("HexDigitsWithHexExponentStart", TokenRules.pattern("[0-9a-fA-F]+[pP][+-]?[0-9]+"));

		// Exponent patterns
		this.builder.defineRule("Exponent", TokenRules.pattern("[eE][+-]?"));
		this.builder.defineRule("HexExponent", TokenRules.pattern("[pP][+-]?"));
		this.builder.defineRule("ExponentWithDigits", TokenRules.pattern("[eE][+-]?[0-9]+"));
		this.builder.defineRule("ExponentWithDigitsAndSuffix", TokenRules.pattern("[eE][+-]?[0-9]+[fFdD]"));
		this.builder.defineRule("HexExponentWithDigits", TokenRules.pattern("[pP][+-]?[0-9]+"));
		this.builder.defineRule("HexExponentWithDigitsAndSuffix", TokenRules.pattern("[pP][+-]?[0-9]+[fFdD]"));

		// Suffix patterns
		this.builder.defineRule("FloatSuffix", TokenRules.pattern("[fFdD]"));
		this.builder.defineRule("LongSuffix", TokenRules.pattern("[lL]"));
	}

	//region Hexadecimal Floating-Point Rules

	/**
	 * Hexadecimal floating-point literals with decimal point.
	 * Examples: 0x1.2p3, 0X1.FP-2, 0x.8p0, 0x1.p5, 0x1_2.3p4, 0x1.2_3p4, 0x1.2p1_0
	 */
	private void addHexFloatWithDotRules() {
		// 0x1_2.3p4 where integer part has underscores and fractional has exponent
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX][0-9a-fA-F]+"),
			TokenRules.value('_', false),
			TokenRules.reference("HexDigits"),
			TokenRules.zeroOrMore(TokenRules.sequence(
				TokenRules.value('_', false),
				TokenRules.reference("HexDigits")
			)),
			TokenRules.value('.', false),
			TokenRules.reference("HexDigitsWithHexExponent"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x1.2_3_4p5 where fractional has underscores and exponent
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX][0-9a-fA-F]+"),
			TokenRules.value('.', false),
			TokenRules.reference("HexDigits"),
			TokenRules.value('_', false),
			TokenRules.reference("HexDigits"),
			TokenRules.zeroOrMore(TokenRules.sequence(
				TokenRules.value('_', false),
				TokenRules.reference("HexDigits")
			)),
			TokenRules.pattern("[pP][+-]?[0-9]+"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x1.2p1_0 where exponent has underscores
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX][0-9a-fA-F]+"),
			TokenRules.value('.', false),
			TokenRules.reference("HexDigitsWithHexExponentStart"),
			TokenRules.value('_', false),
			TokenRules.reference("Digits"),
			TokenRules.zeroOrMore(TokenRules.sequence(
				TokenRules.value('_', false),
				TokenRules.reference("Digits")
			)),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x1.2p3f where fractional has exponent and suffix attached
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX][0-9a-fA-F]+"),
			TokenRules.value('.', false),
			TokenRules.reference("HexDigitsWithHexExponentAndSuffix")
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x.8p0f (no integer part, exponent and suffix attached)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX]"),
			TokenRules.value('.', false),
			TokenRules.reference("HexDigitsWithHexExponentAndSuffix")
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x1.2p3 where fractional has exponent (suffix separate or none)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX][0-9a-fA-F]+"),
			TokenRules.value('.', false),
			TokenRules.reference("HexDigitsWithHexExponent"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x.8p0 (no integer part, fractional has exponent)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX]"),
			TokenRules.value('.', false),
			TokenRules.reference("HexDigitsWithHexExponent"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x1.Fp-2 where exponent sign is separate
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX][0-9a-fA-F]+"),
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9a-fA-F]+[pP]"),
			TokenRules.pattern("[+-]"),
			TokenRules.reference("Digits"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x1.2p3 with integer and fractional parts, exponent separate or together
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX]"),
			TokenRules.reference("HexDigits"),
			TokenRules.value('.', false),
			TokenRules.reference("HexDigits").optional(),
			TokenRules.reference("HexExponent"),
			TokenRules.reference("Digits"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x.2p3 (no integer part, exponent separate)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX]"),
			TokenRules.value('.', false),
			TokenRules.reference("HexDigits"),
			TokenRules.reference("HexExponent"),
			TokenRules.reference("Digits"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));
	}

	/**
	 * Hexadecimal floating-point literals without decimal point.
	 * Examples: 0x1p0, 0xABCp10, 0x1p5f
	 */
	private void addHexFloatNoDotRule() {
		this.builder.addRule(
			TokenRules.pattern("0[xX][0-9a-fA-F]+[pP][+-]?[0-9]+[fFdD]?"),
			TokenActions.grouping(GroupingMode.MATCHED)
		);
	}

	//endregion

	//region Decimal Floating-Point Rules with Exponent

	/**
	 * Decimal floating-point literals with exponent and decimal point.
	 * Examples: 1.23e10, 1.e10, .5e-2, 1.0e1_000
	 */
	private void addDecimalFloatWithExponentAndDotRules() {
		// 1.0e1_000 where exponent has underscores - MUST come first
		this.builder.addRule(TokenRules.sequence(
			TokenRules.reference("Digits"),
			TokenRules.value('.', false),
			TokenRules.reference("DigitsWithExponentStart"),
			TokenRules.value('_', false),
			TokenRules.reference("Digits"),
			TokenRules.zeroOrMore(TokenRules.sequence(
				TokenRules.value('_', false),
				TokenRules.reference("Digits")
			)),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 1.23e10 where fractional has exponent attached
		this.builder.addRule(TokenRules.sequence(
			TokenRules.reference("Digits"),
			TokenRules.value('.', false),
			TokenRules.reference("DigitsWithExponent"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// .5e10 (no integer part, fractional has exponent)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('.', false),
			TokenRules.reference("DigitsWithExponent"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 1.23e-10 where exponent sign is separate
		this.builder.addRule(TokenRules.sequence(
			TokenRules.reference("Digits"),
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9]+[eE]"),
			TokenRules.pattern("[+-]"),
			TokenRules.reference("Digits"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// .5e-2 (no integer part, exponent sign separate)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9]+[eE]"),
			TokenRules.pattern("[+-]"),
			TokenRules.reference("Digits"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 1.23e10 where all parts separate (integer, dot, fractional, exponent sign, exponent digits)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.reference("Digits"),
			TokenRules.value('.', false),
			TokenRules.reference("Digits").optional(),
			TokenRules.reference("Exponent"),
			TokenRules.reference("Digits"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// .5e10 (no integer part, all separate)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('.', false),
			TokenRules.reference("Digits"),
			TokenRules.reference("Exponent"),
			TokenRules.reference("Digits"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));
	}

	/**
	 * Decimal floating-point literals with exponent but no decimal point.
	 * Examples: 1e10, 1E-5, 1e10f
	 */
	private void addDecimalFloatWithExponentNoDotRule() {
		this.builder.addRule(
			TokenRules.pattern("[0-9]+[eE][+-]?[0-9]+[fFdD]?"),
			TokenActions.grouping(GroupingMode.MATCHED)
		);
	}

	//endregion

	//region Decimal Floating-Point Rules without Exponent

	/**
	 * Decimal floating-point literals with decimal point but no exponent.
	 * Examples: 3.14, 3.14f, .5, 123., 123.f, 3.14_15_92
	 */
	private void addDecimalFloatNoDotRules() {
		// 1.23_45_67 with underscores in fractional part
		this.builder.addRule(TokenRules.sequence(
			TokenRules.reference("Digits"),
			TokenRules.value('.', false),
			TokenRules.reference("Digits"),
			TokenRules.value('_', false),
			TokenRules.reference("Digits"),
			TokenRules.zeroOrMore(TokenRules.sequence(
				TokenRules.value('_', false),
				TokenRules.reference("Digits")
			)),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// .5_67_89 (no integer part, with underscores)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('.', false),
			TokenRules.reference("Digits"),
			TokenRules.value('_', false),
			TokenRules.reference("Digits"),
			TokenRules.zeroOrMore(TokenRules.sequence(
				TokenRules.value('_', false),
				TokenRules.reference("Digits")
			)),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 1.23f where suffix is attached to fractional digits
		this.builder.addRule(TokenRules.sequence(
			TokenRules.reference("Digits"),
			TokenRules.value('.', false),
			TokenRules.reference("DigitsWithFloatSuffix")
		), TokenActions.grouping(GroupingMode.ALL));

		// .5f where suffix is attached to fractional digits
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('.', false),
			TokenRules.reference("DigitsWithFloatSuffix")
		), TokenActions.grouping(GroupingMode.ALL));

		// 1.23 with integer and fractional parts (suffix separate or none)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.reference("Digits"),
			TokenRules.value('.', false),
			TokenRules.reference("Digits"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 123. (integer only, no fractional)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.reference("Digits"),
			TokenRules.value('.', false),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL), false);

		// .5 (fractional only, no integer, suffix separate or none)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('.', false),
			TokenRules.reference("Digits"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));
	}

	//endregion

	//region Integer Rules with Underscores

	/**
	 * Octal integers with underscores.
	 * Examples: 0_7_7_7, 07_77L
	 * NOTE: Only matches sequences starting with exactly "0" (single zero token) to avoid
	 * conflicts with decimal literals like 1_000_000.
	 */
	private void addOctalIntWithUnderscoreRule() {
		// Simple case: 0_77L where suffix is attached (starts with exactly "0")
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('0', false),
			TokenRules.value('_', false),
			TokenRules.reference("DigitsWithLongSuffix")
		), TokenActions.grouping(GroupingMode.ALL));

		// Multiple underscores or separate suffix (starts with exactly "0")
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('0', false),
			TokenRules.value('_', false),
			TokenRules.reference("OctalDigits"),
			TokenRules.zeroOrMore(TokenRules.sequence(
				TokenRules.value('_', false),
				TokenRules.reference("OctalDigits")
			)),
			TokenRules.reference("LongSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));
	}

	/**
	 * Hexadecimal integers with underscores.
	 * Examples: 0xDE_AD_BE_EF, 0xFF_FF_FF_FFL
	 */
	private void addHexIntWithUnderscoreRule() {
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX][0-9a-fA-F]+"),
			TokenRules.value('_', false),
			TokenRules.reference("HexDigits"),
			TokenRules.zeroOrMore(TokenRules.sequence(
				TokenRules.value('_', false),
				TokenRules.reference("HexDigits")
			)),
			TokenRules.reference("LongSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));
	}

	/**
	 * Binary integers with underscores.
	 * Examples: 0b1010_1010, 0B1111_0000L
	 */
	private void addBinaryIntWithUnderscoreRule() {
		// Simple case with suffix attached: 0b1111_0000L
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[bB][01]+"),
			TokenRules.value('_', false),
			TokenRules.reference("BinaryDigitsWithLongSuffix")
		), TokenActions.grouping(GroupingMode.ALL));

		// Simple case: 0b1111_0000 (single underscore, suffix separate or none)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[bB][01]+"),
			TokenRules.value('_', false),
			TokenRules.reference("BinaryDigits"),
			TokenRules.reference("LongSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// Multiple underscores: 0b11_00_11_00L
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[bB][01]+"),
			TokenRules.value('_', false),
			TokenRules.reference("BinaryDigits"),
			TokenRules.value('_', false),
			TokenRules.reference("BinaryDigits"),
			TokenRules.zeroOrMore(TokenRules.sequence(
				TokenRules.value('_', false),
				TokenRules.reference("BinaryDigits")
			)),
			TokenRules.reference("LongSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));
	}

	/**
	 * Decimal integers with underscores.
	 * Examples: 1_000_000, 1_000L
	 */
	private void addDecimalIntWithUnderscoreRule() {
		this.builder.addRule(TokenRules.sequence(
			TokenRules.reference("Digits"),
			TokenRules.value('_', false),
			TokenRules.reference("Digits"),
			TokenRules.zeroOrMore(TokenRules.sequence(
				TokenRules.value('_', false),
				TokenRules.reference("Digits")
			)),
			TokenRules.reference("LongSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));
	}

	//endregion

	//region Simple Integer Rules (Pattern-based)

	/**
	 * Hexadecimal integer literals without underscores.
	 * Examples: 0x0, 0x1A, 0XDEADBEEF, 0xFFFFL
	 */
	private void addHexIntRule() {
		this.builder.addRule(
			TokenRules.pattern("0[xX][0-9a-fA-F]+[lL]?"),
			TokenActions.grouping(GroupingMode.MATCHED)
		);
	}

	/**
	 * Binary integer literals without underscores.
	 * Examples: 0b0, 0b1010, 0B1111, 0b1111L
	 */
	private void addBinaryIntRule() {
		this.builder.addRule(
			TokenRules.pattern("0[bB][01]+[lL]?"),
			TokenActions.grouping(GroupingMode.MATCHED)
		);
	}

	/**
	 * Octal integer literals.
	 * Examples: 0, 00, 0777, 012, 0777L
	 */
	private void addOctalIntRule() {
		this.builder.addRule(
			TokenRules.pattern("0[0-7]*[lL]?"),
			TokenActions.grouping(GroupingMode.MATCHED)
		);
	}

	/**
	 * Decimal integer literals without underscores.
	 * Examples: 1, 42, 123, 9223372036854775807L
	 * Note: This must come after octal rule to avoid matching octal as decimal.
	 */
	private void addDecimalIntRule() {
		this.builder.addRule(
			TokenRules.pattern("[1-9][0-9]*[lL]?"),
			TokenActions.grouping(GroupingMode.MATCHED)
		);
	}

	//endregion
}
