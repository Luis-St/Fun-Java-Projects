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

import java.util.regex.Pattern;

/**
 * Grammar definition for matching all Java numeric literals.
 * Supports decimal, hexadecimal, octal, binary integers and floating-point numbers
 * with exponent notation (e/E for decimal, p/P for hexadecimal).
 *
 * @author Luis-St
 */
public class NumericGrammar {

	/**
	 * Regex pattern that matches all Java numeric literal types:
	 * <ul>
	 *   <li>Integer literals: decimal, hexadecimal (0x/0X), octal, binary (0b/0B)</li>
	 *   <li>Floating-point literals: decimal with optional exponent (e/E), hexadecimal with mandatory exponent (p/P)</li>
	 *   <li>Supports underscores as digit separators</li>
	 *   <li>Supports type suffixes: L/l (long), F/f (float), D/d (double)</li>
	 * </ul>
	 */
	public static final String NUMERIC_LITERAL_REGEX =
		"(?:" +
		// Hexadecimal floating-point (0x1.2p3, 0X1.FP-2)
		// Must have at least one hex digit (before or after dot) and requires p/P exponent
		"\\b0[xX](?:[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*(?:\\.(?:[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*)?)?|\\.(?:[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*))" +
		"[pP][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?\\b" +
		"|" +
		// Decimal floating-point with exponent (1.23e10, 4.56E-7, 1e5, .5e2)
		"(?:" +
			// Integer.Fraction with exponent
			"\\b[0-9]+(?:_[0-9]+)*\\.[0-9]+(?:_[0-9]+)*[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?\\b" +
			"|" +
			// Integer. with exponent
			"\\b[0-9]+(?:_[0-9]+)*\\.[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?\\b" +
			"|" +
			// .Fraction with exponent
			"(?<!\\w)\\.[0-9]+(?:_[0-9]+)*[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?\\b" +
			"|" +
			// Integer with exponent (no decimal point)
			"\\b[0-9]+(?:_[0-9]+)*[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?\\b" +
		")" +
		"|" +
		// Decimal floating-point without exponent (1.23, .456, 123.)
		"(?:" +
			// Integer.Fraction
			"\\b[0-9]+(?:_[0-9]+)*\\.[0-9]+(?:_[0-9]+)*[fFdD]?\\b" +
			"|" +
			// Integer. (with optional suffix)
			"\\b[0-9]+(?:_[0-9]+)*\\.(?:[fFdD]\\b|(?!\\w))" +
			"|" +
			// .Fraction
			"(?<!\\w)\\.[0-9]+(?:_[0-9]+)*[fFdD]?\\b" +
		")" +
		"|" +
		// Hexadecimal integer (0x1A, 0X2B, 0xFFFL)
		"\\b0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[lL]?\\b" +
		"|" +
		// Binary integer (0b1010, 0B1111)
		"\\b0[bB][01]+(?:_[01]+)*[lL]?\\b" +
		"|" +
		// Octal integer (0777, 012, 0)
		"\\b0[0-7]*(?:_[0-7]+)*[lL]?\\b" +
		"|" +
		// Decimal integer (123, 42L, 1_000_000)
		"\\b[1-9](?:[0-9]*(?:_[0-9]+)*)?[lL]?\\b" +
		")";

	/**
	 * Compiled pattern for matching Java numeric literals.
	 */
	public static final Pattern NUMERIC_LITERAL_PATTERN = Pattern.compile(NUMERIC_LITERAL_REGEX);

	private NumericGrammar() {
		throw new UnsupportedOperationException("Utility class");
	}
}
