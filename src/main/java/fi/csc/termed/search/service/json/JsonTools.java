package fi.csc.termed.search.service.json;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jmlehtin on 27/4/2017.
 */
public final class JsonTools {

	protected static boolean isEmptyAsString(JsonElement el) {
		return el == null || !el.isJsonPrimitive() || "".equals(el.getAsString());
	}

	protected static boolean isEmptyAsArray(JsonElement el) {
		return el == null || !el.isJsonArray() || el.getAsJsonArray().size() == 0;
	}

	protected static boolean isEmptyAsObject(JsonElement el) {
		return el == null || !el.isJsonObject();
	}

	public static List<String> getIdsFromArrayJsonObjects(List<JsonObject> jsonObjects) {
		if(jsonObjects != null) {
			return jsonObjects.stream().map(obj -> obj.get("id").getAsString()).collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	protected static boolean hasValidGraphId(JsonObject jsonObj) {
		if(	isEmptyAsObject(jsonObj.get("type")) ||
				isEmptyAsObject(jsonObj.getAsJsonObject("type").get("graph")) ||
				isEmptyAsString(jsonObj.getAsJsonObject("type").getAsJsonObject("graph").get("id"))) {
			return false;
		}
		return true;
	}

	protected static boolean hasValidId(JsonObject jsonObj) {
		if(isEmptyAsString(jsonObj.get("id"))) {
			return false;
		}
		return true;
	}

	public static boolean isValidVocabularyJsonForIndex(JsonObject vocabularyJsonObj) {
		if(!hasValidId((vocabularyJsonObj))) {
			return false;
		}

		if(!hasValidGraphId(vocabularyJsonObj)) {
			return false;
		}

		if(	isEmptyAsObject(vocabularyJsonObj.get("properties")) ||
				isEmptyAsArray(vocabularyJsonObj.getAsJsonObject("properties").get("prefLabel")) ||
				isEmptyAsObject(vocabularyJsonObj.getAsJsonObject("properties").getAsJsonArray("prefLabel").get(0)) ||
				isEmptyAsString(vocabularyJsonObj.getAsJsonObject("properties").getAsJsonArray("prefLabel").get(0).getAsJsonObject().get("lang")) ||
				isEmptyAsString(vocabularyJsonObj.getAsJsonObject("properties").getAsJsonArray("prefLabel").get(0).getAsJsonObject().get("value"))) {
			return false;
		}
		return true;
	}

	protected static void setDefinition(JsonObject conceptJsonObj, JsonObject output) {
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

	protected static void setAltLabelsFromPrefLabelArray(JsonArray prefLabelArray, JsonObject altLabelOutputObj) {
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
