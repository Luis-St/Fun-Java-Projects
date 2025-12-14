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

import org.apache.logging.log4j.core.appender.db.jdbc.JdbcDatabaseManager;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import static java.lang.Math.*;
import java.lang.annotation.*;

/**
 * Comprehensive Java syntax test class
 * This class requires Java 22+ to compile due to:
 * - Statements before super()/this() in constructors (Java 22/JEP 447)
 * - Pattern matching in switch (Java 21/JEP 441)
 * - Record patterns (Java 21)
 * - Sealed classes (Java 17)
 *
 * @author Luis-St
 * @version 1.0
 * @since 2024
 */
@SuppressWarnings({"unused", "unchecked"})
@Deprecated(since = "1.0", forRemoval = false)
public class ComprehensiveSyntaxTest<T extends Number & Comparable<T>, U>
	extends AbstractSyntaxTest
	implements Serializable, Cloneable, AutoCloseable {
	
	// Serial version UID
	private static final long serialVersionUID = 1L;
	
	// Primitive types
	private byte byteField = 127;
	private short shortField = 32_767;
	private int intField = 2_147_483_647;
	private long longField = 9_223_372_036_854_775_807L;
	private float floatField = 3.14f;
	private double doubleField = 3.141592653589793d;
	private char charField = 'A';
	private boolean booleanField = true;
	
	// Object types
	private String stringField = "Hello \"World\"";
	private String textBlock = """
            This is a text block
            with multiple lines
            and "quotes" inside
            """;
	
	// Arrays - various declarations
	private int[] intArray = {1, 2, 3, 4, 5};
	private String[] stringArray = new String[] {"a", "b", "c"};
	private int[][] twoDArray = {{1, 2}, {3, 4}};
	private List<String>[] genericArray;
	
	// Access modifiers
	public int publicField;
	protected int protectedField;
	int packagePrivateField;
	private int privateField;
	
	// Modifiers
	public static final int CONSTANT = 42;
	public volatile int volatileField;
	public transient int transientField;
	private static int staticField;
	final int finalField = 100;
	
	// Generics
	private List<String> stringList = new ArrayList<>();
	private Map<String, Integer> stringIntMap = new HashMap<>();
	private List<? extends Number> wildcardList;
	private Map<?, ?> unboundedWildcardMap;
	private List<? super Integer> superWildcard;
	
	// Nested generic types
	private Map<String, List<Map<Integer, Set<Double>>>> complexGeneric;
	
	// Static initializer block
	static {
		staticField = 10;
		System.out.println("Static initializer");
	}
	
	// Instance initializer block
	{
		publicField = 20;
		System.out.println("Instance initializer");
	}
	
	// Nested enum
	public enum Status {
		ACTIVE("Active", 1),
		INACTIVE("Inactive", 0),
		PENDING("Pending", 2) {
			@Override
			public String getDescription() {
				return "Pending status";
			}
		};
		
		private final String label;
		private final int code;
		
		Status(String label, int code) {
			this.label = label;
			this.code = code;
		}
		
		public String getLabel() { return label; }
		public int getCode() { return code; }
		public String getDescription() { return label; }
	}
	
	// Nested interface
	public interface NestedInterface<V> {
		V process(V input);
		default V processWithDefault(V input) {
			return input;
		}
		static void staticMethod() {
			System.out.println("Static interface method");
		}
	}
	
	// Nested static class
	public static class NestedStaticClass {
		private int value;
		
		public NestedStaticClass(int value) {
			this.value = value;
		}
	}
	
	// Inner class
	public class InnerClass {
		private int innerValue;
		
		public void accessOuter() {
			System.out.println(intField);
		}
	}
	
	// Local class in method
	public void methodWithLocalClass() {
		class LocalClass {
			void doSomething() {
				System.out.println("Local class");
			}
		}
		LocalClass local = new LocalClass();
		local.doSomething();
	}
	
	// Anonymous class
	private Runnable anonymousClass = new Runnable() {
		@Override
		public void run() {
			System.out.println("Anonymous class");
		}
	};
	
	// Record (Java 14+)
	public record Point(int x, int y) {
		// Compact constructor
		public Point {
			if (x < 0 || y < 0) {
				throw new IllegalArgumentException("Coordinates must be positive");
			}
		}
		
		// Additional constructor
		public Point() {
			this(0, 0);
		}
		
		// Instance method
		public double distanceFromOrigin() {
			return Math.sqrt(x * x + y * y);
		}
	}
	
	// Sealed class (Java 17+)
	public sealed interface Shape permits Circle, Rectangle, Triangle {}
	
	public static final class Circle implements Shape {
		private final double radius;
		public Circle(double radius) { this.radius = radius; }
	}
	
	public static final class Rectangle implements Shape {
		private final double width, height;
		public Rectangle(double width, double height) {
			this.width = width;
			this.height = height;
		}
	}
	
	public static non-sealed class Triangle implements Shape {
		private final double a, b, c;
		public Triangle(double a, double b, double c) {
			this.a = a; this.b = b; this.c = c;
		}
	}
	
	// Constructors
	public ComprehensiveSyntaxTest() {
		this(42);
	}
	
	public ComprehensiveSyntaxTest(int value) {
		this.intField = value;
	}
	
	// Constructor with varargs
	public ComprehensiveSyntaxTest(String... args) {
		this.intField = args.length;
	}
	
	// Java 22+ feature: Statements before super() call
	// This allows validation and preparation logic before calling parent constructor
	public ComprehensiveSyntaxTest(String name, int value, boolean validate) {
		// Statements before super() - Java 22+ feature
		if (validate && value < 0) {
			throw new IllegalArgumentException("Value must be non-negative");
		}
		
		// Compute or prepare values before super
		int processedValue = value * 2;
		String processedName = name != null ? name.toUpperCase() : "DEFAULT";
		
		// Local variable can be used before super
		int localVar = processedValue + 10;
		System.out.println("Preparing to call super with: " + localVar);
		
		// Now call super
		super();
		
		// Regular constructor body after super
		this.intField = localVar;
		this.stringField = processedName;
	}
	
	// Another example with this() call after statements
	public ComprehensiveSyntaxTest(double doubleValue) {
		// Statements before this() call
		if (doubleValue < 0) {
			System.err.println("Warning: negative value provided");
		}
		
		int convertedValue = (int) Math.abs(doubleValue);
		
		// Now call this()
		this(convertedValue);
	}
	
	// All arithmetic operators
	public void arithmeticOperators() {
		int a = 10, b = 3;
		int add = a + b;
		int subtract = a - b;
		int multiply = a * b;
		int divide = a / b;
		int modulo = a % b;
		
		// Unary operators
		int unaryPlus = +a;
		int unaryMinus = -a;
		int preIncrement = ++a;
		int postIncrement = a++;
		int preDecrement = --a;
		int postDecrement = a--;
	}
	
	// Bitwise operators
	public void bitwiseOperators() {
		int a = 0b1010, b = 0b1100;
		int and = a & b;
		int or = a | b;
		int xor = a ^ b;
		int complement = ~a;
		int leftShift = a << 2;
		int rightShift = a >> 2;
		int unsignedRightShift = a >>> 2;
	}
	
	// Logical operators
	public void logicalOperators() {
		boolean a = true, b = false;
		boolean and = a && b;
		boolean or = a || b;
		boolean not = !a;
		boolean xor = a ^ b;
	}
	
	// Comparison operators
	public void comparisonOperators() {
		int a = 10, b = 20;
		boolean equal = a == b;
		boolean notEqual = a != b;
		boolean greater = a > b;
		boolean less = a < b;
		boolean greaterEqual = a >= b;
		boolean lessEqual = a <= b;
	}
	
	// Assignment operators
	public void assignmentOperators() {
		int a = 10;
		a += 5;  // a = a + 5
		a -= 3;  // a = a - 3
		a *= 2;  // a = a * 2
		a /= 4;  // a = a / 4
		a %= 3;  // a = a % 3
		a &= 0b1010;
		a |= 0b0101;
		a ^= 0b1111;
		a <<= 2;
		a >>= 1;
		a >>>= 1;
	}
	
	// Ternary operator
	public int ternaryOperator(int a, int b) {
		return a > b ? a : b;
	}
	
	// instanceof operator and pattern matching
	public void instanceofOperator(Object obj) {
		// Classic instanceof
		if (obj instanceof String) {
			String s = (String) obj;
			System.out.println(s);
		}
		
		// Pattern matching (Java 16+)
		if (obj instanceof String str) {
			System.out.println(str.toUpperCase());
		}
		
		// Pattern matching with logical operators
		if (obj instanceof String s && s.length() > 0) {
			System.out.println(s);
		}
	}
	
	// Control flow - if/else
	public void ifElseStatement(int value) {
		if (value > 0) {
			System.out.println("Positive");
		} else if (value < 0) {
			System.out.println("Negative");
		} else {
			System.out.println("Zero");
		}
	}
	
	// Switch statement - classic
	public void switchStatement(int day) {
		switch (day) {
			case 1:
				System.out.println("Monday");
				break;
			case 2:
				System.out.println("Tuesday");
				break;
			case 3:
			case 4:
			case 5:
				System.out.println("Weekday");
				break;
			default:
				System.out.println("Weekend");
				break;
		}
	}
	
	// Switch expression (Java 14+)
	public String switchExpression(int day) {
		return switch (day) {
			case 1 -> "Monday";
			case 2 -> "Tuesday";
			case 3, 4, 5 -> "Weekday";
			default -> "Weekend";
		};
	}
	
	// Switch with pattern matching (Java 21+)
	public String switchPatternMatching(Object obj) {
		return switch (obj) {
			case null -> "null";
			case Integer i -> "Integer: " + i;
			case String s -> "String: " + s;
			case Long l when l > 0 -> "Positive long: " + l;
			case Long l -> "Non-positive long: " + l;
			case double[] arr -> "Double array of length " + arr.length;
			default -> "Unknown type";
		};
	}
	
	// For loop - traditional
	public void forLoop() {
		for (int i = 0; i < 10; i++) {
			System.out.println(i);
		}
		
		// Multiple initializers
		for (int i = 0, j = 10; i < j; i++, j--) {
			System.out.println(i + " " + j);
		}
		
		// Infinite loop
		for (;;) {
			break;
		}
	}
	
	// Enhanced for loop
	public void enhancedForLoop() {
		int[] numbers = {1, 2, 3, 4, 5};
		for (int num : numbers) {
			System.out.println(num);
		}
		
		List<String> list = Arrays.asList("a", "b", "c");
		for (String s : list) {
			System.out.println(s);
		}
	}
	
	// While loop
	public void whileLoop() {
		int i = 0;
		while (i < 10) {
			System.out.println(i);
			i++;
		}
	}
	
	// Do-while loop
	public void doWhileLoop() {
		int i = 0;
		do {
			System.out.println(i);
			i++;
		} while (i < 10);
	}
	
	// Break and continue with labels
	public void labeledBreakContinue() {
		outer:
		for (int i = 0; i < 10; i++) {
			inner:
			for (int j = 0; j < 10; j++) {
				if (j == 5) continue inner;
				if (i == 5) break outer;
				System.out.println(i + ", " + j);
			}
		}
	}
	
	// Try-catch-finally
	public void exceptionHandling() {
		try {
			int result = 10 / 0;
		} catch (ArithmeticException e) {
			System.err.println("Arithmetic error");
		} catch (Exception e) {
			System.err.println("General error");
		} finally {
			System.out.println("Finally block");
		}
	}
	
	// Try-with-resources
	public void tryWithResources() {
		try (BufferedReader br = new BufferedReader(new FileReader("test.txt"));
			 BufferedWriter bw = new BufferedWriter(new FileWriter("out.txt"))) {
			String line = br.readLine();
			bw.write(line);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Multi-catch
	public void multiCatch() {
		try {
			Files.delete(Files.createTempFile("temp", ".txt"));
			
			// SQL Query
			Connection con = DriverManager.getConnection("jdbc:h2:mem:");
			Statement stmt = con.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT 1");
			
			// Some code
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
	}
	
	// Throw and throws
	public void throwException() throws IOException, SQLException {
		if (intField < 0) {
			throw new IllegalArgumentException("Value must be positive");
		}
		throw new IOException("IO error");
	}
	
	// Lambda expressions
	public void lambdaExpressions() {
		// No parameters
		Runnable r1 = () -> System.out.println("Hello");
		
		// Single parameter
		Consumer<String> c1 = s -> System.out.println(s);
		Consumer<String> c2 = (s) -> System.out.println(s);
		Consumer<String> c3 = (String s) -> System.out.println(s);
		
		// Multiple parameters
		BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
		BiFunction<Integer, Integer, Integer> multiply = (Integer a, Integer b) -> {
			return a * b;
		};
		
		// Lambda with block
		Predicate<String> isEmpty = s -> {
			return s == null || s.isEmpty();
		};
	}
	
	// Method references
	public void methodReferences() {
		// Static method reference
		Function<String, Integer> parseInt = Integer::parseInt;
		
		// Instance method reference on particular object
		String str = "Hello";
		Supplier<Integer> getLength = str::length;
		
		// Instance method reference on arbitrary object
		Function<String, String> toUpperCase = String::toUpperCase;
		
		// Constructor reference
		Supplier<ArrayList<String>> listSupplier = ArrayList::new;
		Function<Integer, ArrayList<String>> listWithSize = ArrayList::new;
		
		// Array constructor reference
		IntFunction<String[]> arrayCreator = String[]::new;
	}
	
	// Streams
	public void streamOperations() {
		List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		
		// Stream pipeline
		int sum = numbers.stream()
			.filter(n -> n % 2 == 0)
			.map(n -> n * n)
			.reduce(0, Integer::sum);
		
		// Parallel stream
		long count = numbers.parallelStream()
			.filter(n -> n > 5)
			.count();
		
		// Collectors
		List<Integer> filtered = numbers.stream()
			.filter(n -> n % 2 == 0)
			.collect(Collectors.toList());
		
		Map<Boolean, List<Integer>> partitioned = numbers.stream()
			.collect(Collectors.partitioningBy(n -> n % 2 == 0));
		
		String joined = numbers.stream()
			.map(String::valueOf)
			.collect(Collectors.joining(", "));
	}
	
	// Optional
	public void optionalUsage() {
		Optional<String> optional = Optional.of("Hello");
		Optional<String> empty = Optional.empty();
		Optional<String> nullable = Optional.ofNullable(null);
		
		optional.ifPresent(System.out::println);
		String value = optional.orElse("Default");
		String computed = optional.orElseGet(() -> "Computed");
		String required = optional.orElseThrow();
		
		Optional<Integer> length = optional.map(String::length);
		Optional<String> upper = optional.flatMap(s -> Optional.of(s.toUpperCase()));
	}
	
	// Varargs
	public void varargMethod(String... args) {
		for (String arg : args) {
			System.out.println(arg);
		}
	}
	
	public void varargMethod(int count, String... args) {
		System.out.println("Count: " + count);
	}
	
	// Generic method
	public <E> void genericMethod(E element) {
		System.out.println(element);
	}
	
	public <K, V> Map<K, V> createMap(K key, V value) {
		Map<K, V> map = new HashMap<>();
		map.put(key, value);
		return map;
	}
	
	// Method with bounded type parameter
	public <N extends Number> double sumNumbers(List<N> numbers) {
		return numbers.stream()
			.mapToDouble(Number::doubleValue)
			.sum();
	}
	
	// Method with multiple bounds
	public <T extends Comparable<T> & Serializable> T max(T a, T b) {
		return a.compareTo(b) > 0 ? a : b;
	}
	
	// Synchronized method
	public synchronized void synchronizedMethod() {
		System.out.println("Synchronized");
	}
	
	// Synchronized block
	public void synchronizedBlock() {
		synchronized (this) {
			System.out.println("Synchronized block");
		}
		
		Object lock = new Object();
		synchronized (lock) {
			System.out.println("Another sync block");
		}
	}
	
	// Native method declaration
	public native void nativeMethod();
	
	// Strictfp method (deprecated but still valid syntax)
	public strictfp double strictfpMethod(double a, double b) {
		return a * b;
	}
	
	// Assert statement
	public void assertStatement(int value) {
		assert value >= 0 : "Value must be non-negative";
		assert value < 100;
	}
	
	// Annotations
	@Override
	public String toString() {
		return "ComprehensiveSyntaxTest";
	}
	
	@Override
	protected void log(String message) {
	
	}
	
	@Deprecated(since = "1.0", forRemoval = true)
	public void deprecatedMethod() {
		System.out.println("Deprecated");
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void suppressWarningsMethod() {
		List list = new ArrayList();
		list.add("unchecked");
	}
	
	@SafeVarargs
	@SuppressWarnings("varargs")
	public final <T> void safeVarargsMethod(T... args) {
		for (T arg : args) {
			System.out.println(arg);
		}
	}
	
	@FunctionalInterface
	public interface CustomFunctionalInterface<T> {
		void process(T input);
	}
	
	// Nested annotation definition
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
	public @interface NestedAnnotation {
		String name() default "";
		int priority() default 0;
	}
	
	// Custom annotation with nested annotation
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD, ElementType.FIELD})
	public @interface CustomAnnotation {
		String value() default "";
		int count() default 0;
		String[] tags() default {};
		Status status() default Status.ACTIVE;
		NestedAnnotation nested() default @NestedAnnotation(name = "default", priority = 1);
		NestedAnnotation[] nestedArray() default {};
	}
	
	@CustomAnnotation(
		value = "test",
		count = 5,
		tags = {"a", "b"},
		status = Status.ACTIVE,
		nested = @NestedAnnotation(name = "inner", priority = 10),
		nestedArray = {
			@NestedAnnotation(name = "first", priority = 1),
			@NestedAnnotation(name = "second", priority = 2)
		}
	)
	public void annotatedMethod() {
		System.out.println("Annotated");
	}
	
	// Type annotations (Java 8+)
	public void typeAnnotations() {
		@SuppressWarnings("unused")
		String str = "test";
		
		List<@NotNull String> list = new ArrayList<>();
	}
	
	// Different literal formats
	public void literals() {
		// Integer literals
		int decimal = 123;
		int hex = 0x7B;
		int octal = 0173;
		int binary = 0b1111011;
		int withUnderscores = 1_000_000;
		
		// Floating point literals
		double d1 = 123.456;
		double d2 = 1.234e2;
		double d3 = 1.234E-2;
		double d4 = 1_234.567_89;
		double d5 = 0x1.91eb851eb851fp+6;
		float f1 = 123.456f;
		float f2 = 1.234e2f;
		float f3 = 1.234E-2f;
		float f4 = 1_234.567_89f;
		float f5 = 0x1.91eb86p+6f;
		
		// Character literals
		char ch1 = 'A';
		char ch2 = '\n';
		char ch3 = '\u0041';
		char ch4 = 65;
		
		// String literals
		String s1 = "Hello";
		String s2 = "Line1\nLine2";
		String s3 = "Quote: \"text\"";
		String s4 = "Backslash: \\";
		
		// Null literal
		Object obj = null;
	}
	
	// Array operations
	public void arrayOperations() {
		int[] arr1 = new int[10];
		int[] arr2 = {1, 2, 3, 4, 5};
		int[][] arr3 = new int[5][10];
		int[][] arr4 = {{1, 2}, {3, 4}, {5, 6}};
		
		// Array access
		int value = arr2[0];
		arr2[0] = 10;
		
		// Array length
		int length = arr2.length;
		
		// Multi-dimensional array access
		int value2 = arr4[1][0];
	}
	
	// Casting
	public void casting() {
		// Primitive casting
		int i = 100;
		long l = i;  // Implicit widening
		int i2 = (int) l;  // Explicit narrowing
		
		double d = 3.14;
		int i3 = (int) d;  // Truncation
		
		// Reference casting
		Object obj = "Hello";
		String str = (String) obj;  // Downcast
		
		// Generic casting
		List<String> list = (List<String>) new ArrayList<String>();
	}
	
	// This and super
	public void thisAndSuper() {
		this.intField = 10;
		int local = this.intField;
		this.instanceMethod();
		
		// super.someMethod();  // Would call parent's method
	}
	
	// Method overloading
	public void overloadedMethod() {
		System.out.println("No parameters");
	}
	
	public void overloadedMethod(int value) {
		System.out.println("Int parameter: " + value);
	}
	
	public void overloadedMethod(String value) {
		System.out.println("String parameter: " + value);
	}
	
	public void overloadedMethod(int value1, int value2) {
		System.out.println("Two int parameters");
	}
	
	public <T> void overloadedMethod(T value) {
		System.out.println("Generic parameter");
	}
	
	// Nested ternary
	public String nestedTernary(int value) {
		return value > 0 ? "positive" : value < 0 ? "negative" : "zero";
	}
	
	// Complex expression
	public void complexExpression() {
		int result = ((10 + 5) * 3 - 8) / (2 + 3) % 4;
		boolean complex = (result > 0 && result < 100) || (result == -1 && booleanField);
	}
	
	// Instance method
	private void instanceMethod() {
		System.out.println("Instance method");
	}
	
	// Static method
	public static void staticMethod() {
		System.out.println("Static method");
	}
	
	// Final method
	public final void finalMethod() {
		System.out.println("Final method - cannot be overridden");
	}
	
	// Abstract method would need abstract class
	// public abstract void abstractMethod();
	
	// Default method (in interface)
	// Already shown in NestedInterface
	
	// AutoCloseable implementation
	@Override()
	public void close() throws Exception {
		System.out.println("Closing resource");
	}
	
	// Main method
	public static void main(String[] args) {
		ComprehensiveSyntaxTest<Integer, String> test = new ComprehensiveSyntaxTest<>();
		
		// Java 22+ constructors with statements before super/this
		ComprehensiveSyntaxTest<Integer, String> test2 =
			new ComprehensiveSyntaxTest<>("example", 50, true);
		ComprehensiveSyntaxTest<Integer, String> test3 =
			new ComprehensiveSyntaxTest<>(-42.5);
		
		// Method calls
		test.arithmeticOperators();
		test.lambdaExpressions();
		test.streamOperations();
		
		// Array initialization
		String[] varargs = {"a", "b", "c"};
		test.varargMethod(varargs);
		test.varargMethod("x", "y", "z");
		
		// Anonymous class
		Runnable r = new Runnable() {
			@Override
			public void run() {
				System.out.println("Anonymous runnable");
			}
		};
		r.run();
		
		// Lambda
		Runnable r2 = () -> System.out.println("Lambda runnable");
		r2.run();
		
		// Method reference
		Runnable r3 = test::instanceMethod;
		r3.run();
		
		// Record usage
		Point p = new Point(3, 4);
		System.out.println(p.x() + ", " + p.y());
		
		// Enum usage
		Status status = Status.ACTIVE;
		System.out.println(status.getLabel());
		
		// Switch
		System.out.println(test.switchExpression(1));
	}
}

// Abstract parent class (outside main class for clarity)
abstract class AbstractSyntaxTest {
	protected abstract void log(String message);
}

// Separate annotation for use
@interface NonNull {
}
