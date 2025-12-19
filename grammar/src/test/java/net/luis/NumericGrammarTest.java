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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for NumericGrammar pattern.
 *
 * @author Luis-St
 */
class NumericGrammarTest {

	private static final Pattern PATTERN = NumericGrammar.NUMERIC_LITERAL_PATTERN;

	@ParameterizedTest
	@ValueSource(strings = {"0", "1", "9", "42", "123", "2147483647", "9223372036854775807"})
	void testDecimalIntegers(String input) {
		assertTrue(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"0x0", "0x1", "0xF", "0x1A", "0X2B", "0xDEADBEEF", "0xFFFFFFFF", "0xabc", "0xAbC", "0XDEADBEEF"})
	void testHexadecimalIntegers(String input) {
		assertTrue(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"0x", "0X", "0xG", "0xZ"})
	void testInvalidHexadecimalIntegers(String input) {
		assertFalse(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"00", "01", "07", "012", "0777", "01234567"})
	void testOctalIntegers(String input) {
		assertTrue(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"08", "09", "0789"})
	void testInvalidOctalIntegers(String input) {
		assertFalse(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"0b0", "0b1", "0B0", "0B1", "0b1010", "0B1111", "0b11111111"})
	void testBinaryIntegers(String input) {
		assertTrue(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"0b", "0B", "0b2", "0b9", "0bA"})
	void testInvalidBinaryIntegers(String input) {
		assertFalse(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"0.0", "1.0", "3.14", "123.456", ".5", ".123", "123.", "0.", "99.99", "0.001"})
	void testDecimalFloatingPoint(String input) {
		assertTrue(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {".", ".."})
	void testInvalidDecimalFloatingPoint(String input) {
		assertFalse(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"1e10", "1E10", "1e-10", "1E+10", "1.23e10", "1.23E-10", ".5e2", ".5E-2", "123.456e10", "123.e5", "0e0", "9e99"})
	void testDecimalFloatingPointWithExponent(String input) {
		assertTrue(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"e10", "1e", ".e10", "1ee10", "1e10e"})
	void testInvalidExponentNotation(String input) {
		assertFalse(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"0x1p0", "0x1P0", "0x1.0p0", "0x1.2p3", "0X1.FP-2", "0x1.91eb851eb851fp+6", "0x.8p0", "0x1.p5", "0xABCp10", "0x1.FFFp-10", "0x0.0p0"})
	void testHexadecimalFloatingPoint(String input) {
		assertTrue(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"0x1.2", "0x.5", "0xp0", "0x1.2e3"})
	void testInvalidHexadecimalFloatingPoint(String input) {
		assertFalse(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"1_000", "1_000_000", "0x00_FF", "0xDE_AD_BE_EF", "0b1010_1010", "0_777", "1_2_3_4_5"})
	void testIntegersWithUnderscores(String input) {
		assertTrue(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"_123", "123_", "1__000"})
	void testInvalidIntegersWithUnderscores(String input) {
		assertFalse(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"3.14_15_92", "1_234.567_89", "1_000e10", "1.0e1_000", "0x1.FF_FFp10", "1_0.0_1", "0.1_2_3"})
	void testFloatingPointWithUnderscores(String input) {
		assertTrue(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"42L", "42l", "0L", "0l", "0xFFL", "0xFFl", "0b1010L", "0b1010l"})
	void testLongSuffix(String input) {
		assertTrue(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"3.14f", "3.14F", "1e10f", "1e10F", "0x1.2p3F", "0x1.2p3f", ".5f", "5.f"})
	void testFloatSuffix(String input) {
		assertTrue(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {"3.14d", "3.14D", "1e10D", "1e10d", "0x1.2p3d", "0x1.2p3D", ".5d", "5.d"})
	void testDoubleSuffix(String input) {
		assertTrue(matches(input));
	}

	@Test
	void testZeroInAllFormats() {
		assertTrue(matches("0"));
		assertTrue(matches("0L"));
		assertTrue(matches("0.0"));
		assertTrue(matches("0.0f"));
		assertTrue(matches("0x0"));
		assertTrue(matches("0b0"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"9223372036854775807L", "0xFFFFFFFFFFFFFFFF", "0b1111111111111111"})
	void testVeryLargeNumbers(String input) {
		assertTrue(matches(input));
	}

	@ParameterizedTest
	@ValueSource(strings = {".0", "0.", ".00000", "000000."})
	void testMinimalFloatingPoint(String input) {
		assertTrue(matches(input));
	}

	@Test
	void testMultipleUnderscoresInValidPositions() {
		assertTrue(matches("1_000_000_000"));
		assertTrue(matches("3.14_15_92_65_35"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "abc", "1.2.3", "0xG", "0b2", "1ee10", "1e10e", "_123", "123_", "1__000", "1.2e", "0x1.2"})
	void testInvalidNumericLiterals(String input) {
		assertFalse(matches(input));
	}

	@Test
	void testTextWithEmbeddedNumbers() {
		assertFalse(matches("abc123"));
		assertFalse(matches("123abc"));
	}

	@Test
	void testExtractAllLiteralsFromCode() {
		String javaCode = """
			int decimal = 123;
			long hexLong = 0xDEADBEEFL;
			int binary = 0b1010;
			int octal = 0777;
			double pi = 3.14159;
			float e = 2.71828f;
			double scientific = 1.23e-10;
			double hexFloat = 0x1.2p3;
			int million = 1_000_000;
			double precise = 3.14_15_92_65_35;
			""";

		Matcher matcher = PATTERN.matcher(javaCode);

		int expectedCount = 10;
		int actualCount = 0;
		while (matcher.find()) {
			actualCount++;
		}

		assertEquals(expectedCount, actualCount);
	}

	@Test
	void testExtractFromComplexExpressions() {
		String expression = "result = (10 + 0x20) * 3.14 - 0b1010 / 2.5e-3";

		Matcher matcher = PATTERN.matcher(expression);

		String[] expectedLiterals = {"10", "0x20", "3.14", "0b1010", "2.5e-3"};
		int index = 0;

		while (matcher.find()) {
			assertTrue(index < expectedLiterals.length);
			assertEquals(expectedLiterals[index], matcher.group());
			index++;
		}

		assertEquals(expectedLiterals.length, index);
	}

	@Test
	void testBoundaryDetection() {
		String code = "value = 123abc";
		Matcher matcher = PATTERN.matcher(code);

		assertFalse(matcher.find());
	}

	@Test
	void testStandaloneLiterals() {
		String code = "x = 123;";
		Matcher matcher = PATTERN.matcher(code);

		assertTrue(matcher.find());
		assertEquals("123", matcher.group());
	}

	@Test
	void testAllNumberTypesInSingleStatement() {
		String code = "sum = 100 + 0xFF + 0b1010 + 0777 + 3.14 + 1e10 + 0x1.2p3;";
		Matcher matcher = PATTERN.matcher(code);

		String[] expected = {"100", "0xFF", "0b1010", "0777", "3.14", "1e10", "0x1.2p3"};
		int count = 0;

		while (matcher.find()) {
			assertTrue(count < expected.length);
			assertEquals(expected[count], matcher.group());
			count++;
		}

		assertEquals(expected.length, count);
	}

	private boolean matches(String input) {
		return PATTERN.matcher(input).matches();
	}
}
