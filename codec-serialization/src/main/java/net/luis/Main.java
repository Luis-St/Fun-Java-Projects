package net.luis;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.luis.utils.io.codec.Codec;
import net.luis.utils.io.codec.CodecBuilder;
import net.luis.utils.io.codec.mapping.CodecConstructor;
import net.luis.utils.io.codec.mapping.GenericInfo;
import net.luis.utils.io.codec.provider.JsonTypeProvider;
import net.luis.utils.io.data.json.JsonElement;
import net.luis.utils.logging.*;
import net.luis.utils.util.Either;
import org.apache.logging.log4j.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class Main {
	
	private static final Logger LOGGER;
	
	enum State {
		USA, GERMANY, FRANCE, ITALY, SPAIN, UK
	}
	
	static class Address {
		
		final String street;
		final String city;
		final State state;
		final long zip;
		
		Address(String street, String city, State state) {
			this(street, city, state, 0);
		}
		
		@CodecConstructor
		Address(String street, String city, State state, long zip) {
			this.street = street;
			this.city = city;
			this.state = state;
			this.zip = zip;
		}
	}
	
	record Person(
		String name,
		int age,
		double height,
		boolean gender,
		Address[] addresses,
		@GenericInfo(String.class) @NotNull List<String> hobbies,
		@GenericInfo({ String.class, Integer.class }) @NotNull Map<String, Integer> scores,
		@GenericInfo({ List.class, List.class, String.class, Integer.class }) @NotNull Either<List<String>, List<Integer>> contactInfo,
		@GenericInfo(String.class) @NotNull Optional<String> nickname
	) {}
	
	public static void main(String[] args) {
		Codec<Person> codec = CodecBuilder.autoMapCodec(Person.class);
		
		Person person = new Person(
			"John Doe",
			30,
			1.75,
			true,
			new Address[] {
				new Address("123 Main St", "Springfield", State.USA, 12345),
				new Address("456 Elm St", "Shelbyville", State.GERMANY, 67890)
			},
			Lists.newArrayList("Reading", "Traveling", "Cooking"),
			Maps.newHashMap(Map.of("Math", 95, "Science", 90, "History", 85)),
			Either.left(List.of("Mike", "Sarah")),
			Optional.empty()
		);
		
		JsonElement element = codec.encode(JsonTypeProvider.INSTANCE, person);
		LOGGER.debug(element);
		Person decodedPerson = codec.decode(JsonTypeProvider.INSTANCE, element);
		LOGGER.debug(decodedPerson);
	}
	
	static {
		System.setProperty("reflection.exceptions.throw", "true");
		LoggingUtils.initialize(LoggerConfiguration.DEFAULT.disableLogging(LoggingType.FILE).addDefaultLogger(LoggingType.CONSOLE, Level.DEBUG));
		LOGGER = LogManager.getLogger(Main.class);
	}
}
