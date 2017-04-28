package fi.csc.termed.search.service.json;

import com.google.gson.*;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by jmlehtin on 27/4/2017.
 */
public abstract class JsonTools {

	protected String getJsonFileAsString(String filename) {
		try {
			JsonParser parser = new JsonParser();
			return parser.parse(new FileReader(JsonTools.class.getClassLoader().getResource(filename).getFile())).toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (JsonIOException e) {
			e.printStackTrace();
			return null;
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected boolean isEmptyAsString(JsonElement el) {
		return el == null || !el.isJsonPrimitive() || "".equals(el.getAsString());
	}

	protected boolean isEmptyAsArray(JsonElement el) {
		return el == null || !el.isJsonArray() || el.getAsJsonArray().size() == 0;
	}

	protected boolean isEmptyAsObject(JsonElement el) {
		return el == null || !el.isJsonObject();
	}

	public List<String> getIdsFromObjectsInArray(List<JsonObject> jsonObjects) {
		return jsonObjects.stream().map(obj -> obj.get("id").getAsString()).collect(Collectors.toList());
	}

	protected boolean hasValidGraphId(JsonObject jsonObj) {
		if(	isEmptyAsObject(jsonObj.get("type")) ||
				isEmptyAsObject(jsonObj.getAsJsonObject("type").get("graph")) ||
				isEmptyAsString(jsonObj.getAsJsonObject("type").getAsJsonObject("graph").get("id"))) {
			return false;
		}
		return true;
	}

	protected String getVocabularyIdForConcept(JsonObject conceptJsonObj) {
		if(hasValidGraphId(conceptJsonObj)) {
			return conceptJsonObj.getAsJsonObject("type").getAsJsonObject("graph").get("id").getAsString();
		}
		return null;
	}

	protected boolean hasValidId(JsonObject jsonObj) {
		if(isEmptyAsString(jsonObj.get("id"))) {
			return false;
		}
		return true;
	}

	protected String getConceptIdForConcept(JsonObject conceptJsonObj) {
		if(hasValidId(conceptJsonObj)) {
			return conceptJsonObj.get("id").getAsString();
		}
		return null;
	}
	
	protected void setDefinition(JsonObject conceptJsonObj, JsonObject output) {
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

	public boolean setLabelsFromJson(JsonObject inputObj, JsonObject outputObj) {
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

	protected void setAltLabelsFromPrefLabelArray(JsonArray prefLabelArray, JsonObject altLabelOutputObj) {
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
