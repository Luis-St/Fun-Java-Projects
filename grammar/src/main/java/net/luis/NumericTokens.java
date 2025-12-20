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

/**
 * Token definitions for Java numeric literals.
 *
 * @author Luis-St
 */
public interface NumericTokens {

	/**
	 * Regex patterns for matching different Java numeric literal types.
	 */
	interface Patterns {

		/**
		 * Hexadecimal floating-point literals (e.g., 0x1.2p3, 0X1.FP-2).
		 * Must have at least one hex digit and requires p/P exponent.
		 */
		String HEX_FLOAT = "0[xX](?:[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*(?:\\.(?:[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*)?)?|\\.(?:[0-9a-fA-F]+(?:_[0-9a-fA-F]+)*))[pP][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?";

		/**
		 * Decimal floating-point literals with exponent (e.g., 1.23e10, .5e-2).
		 */
		String DECIMAL_FLOAT_EXP = "(?:[0-9]+(?:_[0-9]+)*\\.[0-9]+(?:_[0-9]+)*|[0-9]+(?:_[0-9]+)*\\.|\\.[0-9]+(?:_[0-9]+)*|[0-9]+(?:_[0-9]+)*)[eE][+-]?[0-9]+(?:_[0-9]+)*[fFdD]?";

		/**
		 * Decimal floating-point literals without exponent (e.g., 3.14, .5, 123.).
		 */
		String DECIMAL_FLOAT = "(?:[0-9]+(?:_[0-9]+)*\\.[0-9]+(?:_[0-9]+)*|[0-9]+(?:_[0-9]+)*\\.(?:[fFdD])?|\\.[0-9]+(?:_[0-9]+)*)[fFdD]?";

		/**
		 * Hexadecimal integer literals (e.g., 0x1A, 0XFFFL).
		 */
		String HEX_INT = "0[xX][0-9a-fA-F]+(?:_[0-9a-fA-F]+)*[lL]?";

		/**
		 * Binary integer literals (e.g., 0b1010, 0B1111L).
		 */
		String BINARY_INT = "0[bB][01]+(?:_[01]+)*[lL]?";

		/**
		 * Octal integer literals (e.g., 0777, 012).
		 */
		String OCTAL_INT = "0[0-7]*(?:_[0-7]+)*[lL]?";

		/**
		 * Decimal integer literals (e.g., 123, 42L, 1_000_000).
		 */
		String DECIMAL_INT = "[1-9](?:[0-9]*(?:_[0-9]+)*)?[lL]?";
	}
}
