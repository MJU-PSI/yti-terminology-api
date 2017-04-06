package fi.csc.termed.search.service;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;

/**
 * Created by jmlehtin on 28/3/2017.
 */

@Service
public class JsonParserService {

	public JsonParser getJsonParser() {
		return jsonParser;
	}

	private JsonParser jsonParser;

	Gson gson = new Gson();

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public JsonParserService() {
		this.jsonParser = new JsonParser();
	}

	public String getJsonFileAsString(String filename) {
		try {
			return jsonParser.parse(new FileReader(getClass().getClassLoader().getResource(filename).getFile())).toString();
		} catch (IOException e) {
			log.error("Unable to read file: " + filename);
			return null;
		} catch (JsonIOException e) {
			log.error("Unable to read file as JSON: " + filename);
			return null;
		} catch (JsonSyntaxException e) {
			log.error("Unable to parse file as JSON: " + filename);
			return null;
		}
	}

	public JsonObject transformApiConceptToIndexConcept(JsonObject conceptJsonObj) {
		if(isValidJsonForIndex(conceptJsonObj)) {
			JsonObject output = new JsonObject();
			JsonArray outputBroaderArray = new JsonArray();
			JsonObject outputDefinitionObj = new JsonObject();
			JsonObject outputLabelObj = new JsonObject();
			JsonObject outputAltLabelObj = new JsonObject();
			output.add("broader", outputBroaderArray);
			output.add("definition", outputDefinitionObj);
			output.add("label", outputLabelObj);
			output.add("altLabel", outputAltLabelObj);

			output.addProperty("id", conceptJsonObj.get("id").getAsString());
			output.addProperty("graphId", conceptJsonObj.getAsJsonObject("type").getAsJsonObject("graph").get("id").getAsString());

			if(!isEmptyAsString(conceptJsonObj.get("lastModifiedDate"))) {
				output.addProperty("modified", conceptJsonObj.get("lastModifiedDate").getAsString());
			}

			if(!isEmptyAsObject(conceptJsonObj.get("references")) && !isEmptyAsArray(conceptJsonObj.getAsJsonObject("references").get("broader"))) {
				JsonArray broaderArray = conceptJsonObj.getAsJsonObject("references").getAsJsonArray("broader");
				for(JsonElement broaderElem : broaderArray) {
					if(!isEmptyAsObject(broaderElem) && !isEmptyAsString(broaderElem.getAsJsonObject().get("id"))) {
						outputBroaderArray.add(broaderElem.getAsJsonObject().get("id").getAsString());
					}
				}
			}

			if(	!isEmptyAsObject(conceptJsonObj.get("properties")) &&
				!isEmptyAsArray(conceptJsonObj.getAsJsonObject("properties").get("status"))) {

				output.addProperty("status", conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("status").get(0).getAsJsonObject().get("value").getAsString());
			}

			if(	!isEmptyAsObject(conceptJsonObj.get("properties")) &&
				!isEmptyAsArray(conceptJsonObj.getAsJsonObject("properties").get("definition"))) {

				JsonArray definitionArray = conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("definition");
				for(JsonElement definitionElem : definitionArray) {
					if(!isEmptyAsObject(definitionElem)) {
						if(!isEmptyAsString(definitionElem.getAsJsonObject().get("lang")) && !isEmptyAsString(definitionElem.getAsJsonObject().get("value"))) {
							outputDefinitionObj.addProperty(definitionElem.getAsJsonObject().get("lang").getAsString(), definitionElem.getAsJsonObject().get("value").getAsString());
						}
					}
				}
			}

			boolean labelExists = setLabelFromConceptJson(conceptJsonObj, outputLabelObj);

			if(	!labelExists &&
				!isEmptyAsObject(conceptJsonObj.get("references")) &&
				!isEmptyAsArray(conceptJsonObj.getAsJsonObject("references").get("prefLabelXl"))) {

				JsonArray prefLabelXlArray = conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl");
				for(JsonElement prefLabelXlElem : prefLabelXlArray) {
					if(!isEmptyAsObject(prefLabelXlElem)) {
						setLabelFromConceptJson(prefLabelXlElem.getAsJsonObject(), outputLabelObj);
					}
				}
			}

			if(!isEmptyAsObject(conceptJsonObj.get("references")) &&
					!isEmptyAsArray(conceptJsonObj.getAsJsonObject("references").get("altLabelXl"))) {

				JsonArray altLabelXlArray = conceptJsonObj.getAsJsonObject("references").getAsJsonArray("altLabelXl");
				for(JsonElement altLabelXlElem : altLabelXlArray) {
					if(	!isEmptyAsObject(altLabelXlElem) &&
						!isEmptyAsObject(altLabelXlElem.getAsJsonObject().get("properties")) &&
						!isEmptyAsArray(altLabelXlElem.getAsJsonObject().getAsJsonObject("properties").get("prefLabel"))) {

						JsonArray prefLabelArrayInAltLabelXl = altLabelXlElem.getAsJsonObject().getAsJsonObject("properties").getAsJsonArray("prefLabel");
						JsonObject altLabelObjInOutput = output.getAsJsonObject("altLabel");

						for(JsonElement prefLabelInAltLabelXlElem : prefLabelArrayInAltLabelXl) {
							if(!isEmptyAsObject(prefLabelInAltLabelXlElem)) {
								if(!isEmptyAsString(prefLabelInAltLabelXlElem.getAsJsonObject().get("lang")) && !isEmptyAsString(prefLabelInAltLabelXlElem.getAsJsonObject().get("value"))) {
									if (altLabelObjInOutput.getAsJsonArray(prefLabelInAltLabelXlElem.getAsJsonObject().get("lang").getAsString()) == null) {
										altLabelObjInOutput.add(prefLabelInAltLabelXlElem.getAsJsonObject().get("lang").getAsString(), new JsonArray());
									}
									JsonArray altLabelInLangArrayOutput = altLabelObjInOutput.getAsJsonArray(prefLabelInAltLabelXlElem.getAsJsonObject().get("lang").getAsString());
									altLabelInLangArrayOutput.add(prefLabelInAltLabelXlElem.getAsJsonObject().get("value").getAsString());
								}
							}
						}
					}
				}
			}
			log.error(output.toString());
			return output;
		}
		log.warn("Unable to transform JSON from termed API to JSON required by elasticsearch for " + conceptJsonObj.get("id").getAsString());
		return null;
	}

	private boolean setLabelFromConceptJson(JsonObject conceptObj, JsonObject outputObj) {
		boolean prefLabelAdded = false;
		if(	!isEmptyAsObject(conceptObj.get("properties")) &&
			!isEmptyAsArray(conceptObj.getAsJsonObject("properties").get("prefLabel"))) {

			JsonArray prefLabelArray = conceptObj.getAsJsonObject("properties").getAsJsonArray("prefLabel");
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

	private boolean isValidJsonForIndex(JsonObject conceptJsonObj) {

		if(isEmptyAsString(conceptJsonObj.get("id"))) {
			return false;
		}

		if(	isEmptyAsObject(conceptJsonObj.get("type")) ||
			isEmptyAsObject(conceptJsonObj.getAsJsonObject("type").get("graph")) ||
			isEmptyAsString(conceptJsonObj.getAsJsonObject("type").getAsJsonObject("graph").get("id"))) {
			return false;
		}

		if( (	isEmptyAsObject(conceptJsonObj.get("properties")) ||
				isEmptyAsArray(conceptJsonObj.getAsJsonObject("properties").get("prefLabel")) ||
				isEmptyAsObject(conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("prefLabel").get(0)) ||
				isEmptyAsString(conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("prefLabel").get(0).getAsJsonObject().get("lang")) ||
				isEmptyAsString(conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("prefLabel").get(0).getAsJsonObject().get("value"))
			)
		||
			(	isEmptyAsObject(conceptJsonObj.get("references")) ||
				isEmptyAsArray(conceptJsonObj.getAsJsonObject("references").get("prefLabelXl")) ||
				isEmptyAsObject(conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl").get(0)) ||
				isEmptyAsObject(conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl").get(0).getAsJsonObject().get("properties")) ||
				isEmptyAsArray(conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl").get(0).getAsJsonObject().getAsJsonObject("properties").get("prefLabel")) ||
				isEmptyAsObject(conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl").get(0).getAsJsonObject().getAsJsonObject("properties").getAsJsonArray("prefLabel").get(0)) ||
				isEmptyAsString(conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl").get(0).getAsJsonObject().getAsJsonObject("properties").getAsJsonArray("prefLabel").get(0).getAsJsonObject().get("lang")) ||
				isEmptyAsString(conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl").get(0).getAsJsonObject().getAsJsonObject("properties").getAsJsonArray("prefLabel").get(0).getAsJsonObject().get("value"))
			)
		) {
			return true;
		}
		return false;
	}

	private boolean isEmptyAsString(JsonElement el) {
		return el == null || !el.isJsonPrimitive() || "".equals(el.getAsString());
	}

	private boolean isEmptyAsArray(JsonElement el) {
		return el == null || !el.isJsonArray() || el.getAsJsonArray().size() == 0;
	}

	private boolean isEmptyAsObject(JsonElement el) {
		return el == null || !el.isJsonObject();
	}
}