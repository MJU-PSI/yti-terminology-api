package fi.csc.termed.search.service.json;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class JsonTools {

	static boolean isEmptyAsString(JsonElement el) {
		return el == null || !el.isJsonPrimitive() || "".equals(el.getAsString());
	}

	static boolean isEmptyAsArray(JsonElement el) {
		return el == null || !el.isJsonArray() || el.getAsJsonArray().size() == 0;
	}

	static boolean isEmptyAsObject(JsonElement el) {
		return el == null || !el.isJsonObject();
	}

	public static List<String> getIdsFromArrayJsonObjects(List<JsonObject> jsonObjects) {
		if(jsonObjects != null) {
			return jsonObjects.stream().map(obj -> obj.get("id").getAsString()).collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	static boolean hasValidGraphId(JsonObject jsonObj) {
		return !(isEmptyAsObject(jsonObj.get("type")) ||
				isEmptyAsObject(jsonObj.getAsJsonObject("type").get("graph")) ||
				isEmptyAsString(jsonObj.getAsJsonObject("type").getAsJsonObject("graph").get("id")));
	}

	static boolean hasValidId(JsonObject jsonObj) {
		return !isEmptyAsString(jsonObj.get("id"));
	}

	public static boolean isValidVocabularyJsonForIndex(JsonObject vocabularyJsonObj) {

		boolean hasPrefLabel =
				!(isEmptyAsObject(vocabularyJsonObj.get("properties")) ||
						isEmptyAsArray(vocabularyJsonObj.getAsJsonObject("properties").get("prefLabel")) ||
						isEmptyAsObject(vocabularyJsonObj.getAsJsonObject("properties").getAsJsonArray("prefLabel").get(0)) ||
						isEmptyAsString(vocabularyJsonObj.getAsJsonObject("properties").getAsJsonArray("prefLabel").get(0).getAsJsonObject().get("lang")) ||
						isEmptyAsString(vocabularyJsonObj.getAsJsonObject("properties").getAsJsonArray("prefLabel").get(0).getAsJsonObject().get("value")));

		return hasValidId(vocabularyJsonObj) && hasValidGraphId(vocabularyJsonObj) && hasPrefLabel;
	}

	static void setDefinition(JsonObject conceptJsonObj, JsonObject output) {
		if(	!isEmptyAsObject(conceptJsonObj.get("properties")) &&
				!isEmptyAsArray(conceptJsonObj.getAsJsonObject("properties").get("definition"))) {

			JsonArray definitionArray = conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("definition");
			for(JsonElement definitionElem : definitionArray) {
				if(!isEmptyAsObject(definitionElem)) {
					if(!isEmptyAsString(definitionElem.getAsJsonObject().get("lang")) && !isEmptyAsString(definitionElem.getAsJsonObject().get("value"))) {
						output.addProperty(definitionElem.getAsJsonObject().get("lang").getAsString(), definitionElem.getAsJsonObject().get("value").getAsString());
					}
				}
			}
		}
	}

	public static boolean setLabelsFromJson(JsonObject inputObj, JsonObject outputObj) {
		boolean prefLabelAdded = false;
		if(	!isEmptyAsObject(inputObj.get("properties")) &&
				!isEmptyAsArray(inputObj.getAsJsonObject("properties").get("prefLabel"))) {

			JsonArray prefLabelArray = inputObj.getAsJsonObject("properties").getAsJsonArray("prefLabel");
			for(JsonElement prefLabelElem : prefLabelArray) {
				if(!isEmptyAsObject(prefLabelElem)) {
					JsonObject prefLabelObj = prefLabelElem.getAsJsonObject();
					if (!isEmptyAsString(prefLabelObj.get("lang")) && !isEmptyAsString(prefLabelObj.get("value"))) {
						outputObj.addProperty(prefLabelObj.get("lang").getAsString(), prefLabelObj.get("value").getAsString());
						prefLabelAdded = true;
					}
				}
			}
		}
		return prefLabelAdded;
	}

	static void setAltLabelsFromPrefLabelArray(JsonArray prefLabelArray, JsonObject altLabelOutputObj) {
		for (JsonElement prefLabelInAltLabel : prefLabelArray) {
			if (!isEmptyAsObject(prefLabelInAltLabel)) {
				if (!isEmptyAsString(prefLabelInAltLabel.getAsJsonObject().get("lang")) && !isEmptyAsString(prefLabelInAltLabel.getAsJsonObject().get("value"))) {
					if (altLabelOutputObj.getAsJsonArray(prefLabelInAltLabel.getAsJsonObject().get("lang").getAsString()) == null) {
						altLabelOutputObj.add(prefLabelInAltLabel.getAsJsonObject().get("lang").getAsString(), new JsonArray());
					}
					JsonArray altLabelInLangArrayOutput = altLabelOutputObj.getAsJsonArray(prefLabelInAltLabel.getAsJsonObject().get("lang").getAsString());
					altLabelInLangArrayOutput.add(prefLabelInAltLabel.getAsJsonObject().get("value").getAsString());
				}
			}
		}
	}
}
