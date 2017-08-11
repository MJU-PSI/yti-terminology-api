package fi.csc.termed.api.util;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

public final class JsonUtils {

	private JsonUtils() {
		// prevent construction
	}

	public static @NotNull JsonArray listToJson(@NotNull List<String> collection) {
		JsonArray result = new JsonArray();
		collection.forEach(result::add);
		return result;
	}

	public static @NotNull List<String> jsonToList(@NotNull JsonArray json) {
		return asStream(json).map(JsonElement::getAsString).collect(toList());
	}

	public static @NotNull JsonObject localizableToJson(@NotNull Map<String, List<String>> localizable) {

		JsonObject result = new JsonObject();

		for (Map.Entry<String, List<String>> entry : localizable.entrySet()) {

			String language = entry.getKey();
			List<String> localizations = entry.getValue();
			result.add(language, listToJson(localizations));
		}

		return result;
	}

	public static @NotNull Map<String, List<String>> jsonToLocalizable(@NotNull JsonObject json) {

		Map<String, List<String>> result = new LinkedHashMap<>();

		for (Map.Entry<String, JsonElement> entry : json.entrySet()) {

			String language = entry.getKey();
			List<String> localizations = jsonToList(entry.getValue().getAsJsonArray());
			result.put(language, localizations);
		}

		return result;
	}

	public static @NotNull Map<String, List<String>> localizableFromTermReferences(@NotNull List<JsonObject> termReferences, @NotNull String property) {

		return termReferences.stream()
				.map(JsonElement::getAsJsonObject)
				.flatMap(x -> {
					JsonObject properties = x.get("properties").getAsJsonObject();

					if (properties.has(property)) {
						return asStream(properties.get(property).getAsJsonArray());
					} else {
						return Stream.empty();
					}
				})
				.map(JsonElement::getAsJsonObject)
				.collect(groupingBy(localization -> localization.get("lang").getAsString(),
						mapping(localization -> localization.get("value").getAsString(), toList())));
	}

	public static @NotNull Map<String, List<String>> localizableFromTermedProperties(@NotNull JsonObject properties, @NotNull String property) {
		if (properties.has(property)) {
			return localizableFromTermedProperties(properties.get(property).getAsJsonArray());
		} else {
			return Collections.emptyMap();
		}
	}

	private static @NotNull Map<String, List<String>> localizableFromTermedProperties(@NotNull JsonArray property) {
		return asStream(property)
				.map(JsonElement::getAsJsonObject)
				.collect(groupingBy(localization -> localization.get("lang").getAsString(),
						mapping(localization -> localization.get("value").getAsString(), toList())));
	}

	public static @Nullable String getSinglePropertyValue(@NotNull JsonObject properties, @NotNull String propertyName) {
		if (properties.has(propertyName)) {
			JsonArray property = properties.get(propertyName).getAsJsonArray();

			if (property.size() == 0) {
				return null;
			} else {
				return property.get(0).getAsJsonObject().get("value").getAsString();
			}
		} else {
			return null;
		}
	}

	public static @NotNull Stream<JsonElement> asStream(@NotNull JsonArray array) {
		return StreamSupport.stream(Spliterators.spliterator(array.iterator(), array.size(), Spliterator.ORDERED), false);
	}
}
