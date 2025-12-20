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
 * (for literals containing '.' which is a separator character in the TokenReader).
 * Underscores within numeric literals are handled in the pattern rules using (?:_[0-9]+)* expressions.
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

		// Integer rules (both with and without underscores)
		this.addOctalIntRules();
		this.addHexIntRules();
		this.addBinaryIntRules();
		this.addDecimalIntRules();
	}

	/**
	 * Defines reusable patterns for numeric components.
	 */
	private void defineNumericPatterns() {
		// Exponent patterns (sign without digits)
		this.builder.defineRule("Exponent", TokenRules.pattern("[eE][+-]?"));
		this.builder.defineRule("HexExponent", TokenRules.pattern("[pP][+-]?"));

		// Suffix pattern
		this.builder.defineRule("FloatSuffix", TokenRules.pattern("[fFdD]"));
	}

	//region Hexadecimal Floating-Point Rules

	/**
	 * Hexadecimal floating-point literals with decimal point.
	 * Examples: 0x1.2p3, 0X1.FP-2, 0x.8p0, 0x1.p5, 0x1_2.3p4, 0x1.2_3p4, 0x1.2p1_0
	 */
	private void addHexFloatWithDotRules() {
		// 0x1.2p3 with integer and fractional parts (handles underscores everywhere)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*"),
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[pP][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?")
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x1.p3 (no fractional part, handles underscores in integer and exponent)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*"),
			TokenRules.value('.', false),
			TokenRules.pattern("[pP][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?")
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x.2p3 (no integer part, handles underscores in fractional and exponent)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX]"),
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[pP][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?")
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x1.2p-3 where sign is separate token (0x1 . 2p - 3)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*"),
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[pP]"),
			TokenRules.pattern("[+-]"),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x1.p-3 (no fractional, sign is separate token)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*"),
			TokenRules.value('.', false),
			TokenRules.pattern("[pP]"),
			TokenRules.pattern("[+-]"),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x.2p-3 (no integer, sign is separate token)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX]"),
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[pP]"),
			TokenRules.pattern("[+-]"),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x1.2p3 where exponent components are separate (0x1 . 2 p +/- 3) - sign attached to p
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*"),
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*").optional(),
			TokenRules.reference("HexExponent"),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 0x.2p3 (no integer part, exponent components separate)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("0[xX]"),
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*"),
			TokenRules.reference("HexExponent"),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));
	}

	/**
	 * Hexadecimal floating-point literals without decimal point.
	 * Examples: 0x1p0, 0xABCp10, 0x1p5f, 0x1_ABCp1_0
	 */
	private void addHexFloatNoDotRule() {
		this.builder.addRule(
			TokenRules.pattern("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[pP][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?"),
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
		// 1.23e10 with integer and fractional parts (handles underscores everywhere)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?")
		), TokenActions.grouping(GroupingMode.ALL));

		// 1.e10 (no fractional part, handles underscores in exponent)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.value('.', false),
			TokenRules.pattern("[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?")
		), TokenActions.grouping(GroupingMode.ALL));

		// .5e10 (no integer part, handles underscores in fractional and exponent)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?")
		), TokenActions.grouping(GroupingMode.ALL));

		// 1.23e-10 where sign is separate token (1 . 23e - 10)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*[eE]"),
			TokenRules.pattern("[+-]"),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 1.e-10 (no fractional, sign is separate token)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.value('.', false),
			TokenRules.pattern("[eE]"),
			TokenRules.pattern("[+-]"),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// .5e-10 (no integer, sign is separate token)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*[eE]"),
			TokenRules.pattern("[+-]"),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// 1.23e10 where exponent components are separate (1 . 23e +/- 10) - sign attached to e
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*").optional(),
			TokenRules.reference("Exponent"),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));

		// .5e10 (no integer part, exponent components separate)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.reference("Exponent"),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL));
	}

	/**
	 * Decimal floating-point literals with exponent but no decimal point.
	 * Examples: 1e10, 1E-5, 1e10f, 1_000e1_0
	 */
	private void addDecimalFloatWithExponentNoDotRule() {
		this.builder.addRule(
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?"),
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
		// 1.23 with integer and fractional parts (handles underscores in fractional)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*[fFdD]?")
		), TokenActions.grouping(GroupingMode.ALL));

		// 123. (integer only, no fractional)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*"),
			TokenRules.value('.', false),
			TokenRules.reference("FloatSuffix").optional()
		), TokenActions.grouping(GroupingMode.ALL), false);

		// .5 (fractional only, no integer)
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('.', false),
			TokenRules.pattern("[0-9]+(?:_[0-9]+)*[fFdD]?")
		), TokenActions.grouping(GroupingMode.ALL));
	}

	//endregion

	//region Integer Rules

	/**
	 * Octal integer literals (with and without underscores).
	 * Examples: 0, 00, 0777, 012, 0_7_7_7, 07_77L
	 */
	private void addOctalIntRules() {
		this.builder.addRule(
			TokenRules.pattern("0[0-7]*(?:_[0-7]+)*[lL]?"),
			TokenActions.grouping(GroupingMode.MATCHED)
		);
	}

	/**
	 * Hexadecimal integer literals (with and without underscores).
	 * Examples: 0x0, 0x1A, 0XDEADBEEF, 0xFFFFL, 0xDE_AD_BE_EF
	 */
	private void addHexIntRules() {
		this.builder.addRule(
			TokenRules.pattern("0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[lL]?"),
			TokenActions.grouping(GroupingMode.MATCHED)
		);
	}

	/**
	 * Binary integer literals (with and without underscores).
	 * Examples: 0b0, 0b1010, 0B1111, 0b1111L, 0b1010_1010, 0B1111_0000L
	 */
	private void addBinaryIntRules() {
		this.builder.addRule(
			TokenRules.pattern("0[bB][01]+(?:_[01]+)*[lL]?"),
			TokenActions.grouping(GroupingMode.MATCHED)
		);
	}

	/**
	 * Decimal integer literals (with and without underscores).
	 * Examples: 1, 42, 123, 9223372036854775807L, 1_000_000, 1_000L
	 * Note: Must come after octal rule to avoid matching octal as decimal.
	 */
	private void addDecimalIntRules() {
		this.builder.addRule(
			TokenRules.pattern("[1-9][0-9]*(?:_[0-9]+)*[lL]?"),
			TokenActions.grouping(GroupingMode.MATCHED)
		);
	}

	//endregion
}
