package fi.vm.yti.terminology.api.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

public final class JsonUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);

	private JsonUtils() {
		// prevent construction
	}

    public static @NotNull JsonNode requireSingle(JsonNode array) {
        return requireNonNull(findSingle(array), "One array item required, was: " + array.size());
    }

    public static @Nullable JsonNode findSingle(@Nullable JsonNode array) {

	    if (array == null) {
	      throw new RuntimeException("Missing node");
        } else if (array.size() > 1) {
            throw new RuntimeException("One or zero array items required, was: " + array.size());
        } else if (array.size() == 0) {
            return null;
        } else {
            return array.get(0);
        }
    }

	public static @Nullable JsonNode findSingleOrNull(@Nullable JsonNode array) {
		if (array == null || array.size() == 0) {
			return null;
		} else {
			return array.get(0);
		}
	}

	public static JsonNode listToJson(ObjectMapper mapper, @NotNull List<UUID> collection) {
        ArrayNode arrayNode = mapper.createArrayNode();
        collection.forEach(item -> arrayNode.add(item.toString()));
        return arrayNode;
	}

    public static @NotNull List<UUID> jsonToList(@NotNull JsonNode arrayNode) {
        Stream<JsonNode> stream = StreamSupport.stream(arrayNode.spliterator(), false);
        return stream.map(node -> UUID.fromString(node.textValue())).collect(toList());
    }

	public static @NotNull ObjectNode localizableToJson(ObjectMapper mapper, @NotNull Map<String, List<String>> localizable) {
	    return mapper.valueToTree(localizable);
	}

	public static @NotNull Map<String, List<String>> jsonToLocalizable(@NotNull ObjectMapper mapper, @NotNull JsonNode json) {
        try {
            return mapper.readValue(mapper.treeAsTokens(json), new TypeReference<Map<String, List<String>>>() {});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
	}

	public static @NotNull Map<String, List<String>> localizableFromTermReferences(@NotNull List<JsonNode> termReferences, @NotNull String property) {

		return termReferences.stream()
				.flatMap(x -> {

				    JsonNode properties = x.get("properties");

					if (properties.has(property)) {
						return asStream(properties.get(property));
					} else {
						return Stream.empty();
					}
				})
				.collect(groupingBy(localization -> localization.get("lang").asText(),
						mapping(localization -> localization.get("value").asText(), toList())));
	}

	public static @NotNull Map<String, List<String>> localizableFromTermedProperties(@NotNull JsonNode properties, @NotNull String property) {
		if (properties.has(property)) {
			return localizableFromTermedProperties(properties.get(property));
		} else {
			return Collections.emptyMap();
		}
	}

	private static @NotNull Map<String, List<String>> localizableFromTermedProperties(@NotNull JsonNode property) {
		return asStream(property)
				.collect(groupingBy(localization -> localization.get("lang").textValue(),
						mapping(localization -> localization.get("value").textValue(), toList())));
	}

	public static @Nullable String getSinglePropertyValue(@NotNull JsonNode properties, @NotNull String propertyName) {
		if (properties.has(propertyName)) {
			JsonNode property = properties.get(propertyName);

			if (property.size() == 0) {
				return null;
			} else {
				return property.get(0).get("value").textValue();
			}
		} else {
			return null;
		}
	}

	public static @NotNull Stream<JsonNode> asStream(@Nullable JsonNode node) {
	    if (node == null) {
	        return Stream.empty();
        } else {
            return StreamSupport.stream(node.spliterator(), false);
        }
	}

	public static void prettyPrintJson(Object node){
		ObjectMapper mapper = new ObjectMapper();
		try {
			LOGGER.debug(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node));
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			LOGGER.error("Error on prettyPrintJson", e);
		}
	}

	public static String prettyPrintJsonAsString(Object node){		
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			LOGGER.error("Error on prettyPrintJsonAsString", e);
		}
		return "";
	}

	public static JsonNode sortedFromTermedProperties(JsonNode array, String language, String[] languages) {
		ObjectMapper mapper = new ObjectMapper();
		List<String> langsSortedByOrder = Arrays.stream(languages).filter(s -> !s.equals(language)).collect(Collectors.toList());
		langsSortedByOrder.add(0, language);

		List<JsonNode> nodeArray = StreamSupport.stream(array.spliterator(), false).sorted((t1, t2) -> {
			var t1prefLabels = localizableFromTermedProperties(t1.get("properties"), "prefLabel");
			var t2prefLabels = localizableFromTermedProperties(t2.get("properties"), "prefLabel");

			if (t1prefLabels.isEmpty() || t2prefLabels.isEmpty()) {
				return 0;
			}

			List<String> t1Label = t1prefLabels.get(langsSortedByOrder.stream()
					.filter(s -> t1prefLabels.get(s) != null)
					.findFirst().orElse(null));
			List<String> t2Label = t2prefLabels.get(langsSortedByOrder.stream()
					.filter(s -> t2prefLabels.get(s) != null)
					.findFirst().orElse(null));

			if (t1Label != null && t2Label != null) {
				return t1Label.get(0).compareTo(t2Label.get(0));
			} else {
				return 0;
			}
		}).map(node -> {
			var nodePrefLabels = localizableFromTermedProperties(node.get("properties"), "prefLabel");

			for (var lang : langsSortedByOrder) {
				if (lang.equals(language) && nodePrefLabels.get(language) != null) {
					String newPrefLabel = "{\"lang\":\"" + language
										+ "\", \"value\":\"" + nodePrefLabels.get(language).get(0)
										+ "\", \"regex\":\"(?s)^.*$\"}";
					try {
						((ObjectNode)node.get("properties")).replace("prefLabel", mapper.readTree(newPrefLabel));
						break;
					} catch (JsonProcessingException e) {
						LOGGER.error("Error on sortedFromTermedProperties", e);
					}
				} else if (nodePrefLabels.get(lang) != null) {
					String newPrefLabel = "{\"lang\":\"" + lang
							+ "\", \"value\":\"" + nodePrefLabels.get(lang).get(0) + " (" + lang + ")"
							+ "\", \"regex\":\"(?s)^.*$\"}";
					try {
						((ObjectNode)node.get("properties")).replace("prefLabel", mapper.readTree(newPrefLabel));
						break;
					} catch (JsonProcessingException e) {
						LOGGER.error("Error on sortedFromTermedProperties", e);
					}
				}
			}

			return node;
		}).collect(Collectors.toList());

		return mapper.valueToTree(nodeArray);
	}
}
