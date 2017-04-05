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

			if(conceptJsonObj.get("lastModifiedDate") != null) {
				output.addProperty("modified", conceptJsonObj.get("lastModifiedDate").getAsString());
			}

			if(conceptJsonObj.getAsJsonObject("references").getAsJsonArray("broader") != null && conceptJsonObj.getAsJsonObject("references").getAsJsonArray("broader").size() > 0) {
				JsonArray broaderArray = conceptJsonObj.getAsJsonObject("references").getAsJsonArray("broader");
				for(JsonElement broaderElem : broaderArray) {
					outputBroaderArray.add(broaderElem.getAsJsonObject().get("id").getAsString());
				}
			}

			if(conceptJsonObj.getAsJsonObject("properties") != null &&
					conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("status") != null &&
					conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("status").size() > 0) {

				output.addProperty("status", conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("status").get(0).getAsJsonObject().get("value").getAsString());
			}

			if(conceptJsonObj.getAsJsonObject("properties") != null &&
					conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("definition") != null &&
					conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("definition").size() > 0) {

				JsonArray definitionArray = conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("definition");
				for(JsonElement definitionElem : definitionArray) {
					JsonObject definitionObj = definitionElem.getAsJsonObject();
					outputDefinitionObj.addProperty(definitionObj.get("lang").getAsString(), definitionObj.get("value").getAsString());
				}
			}

			setLabelFromConceptJson(conceptJsonObj, outputLabelObj);

			if(conceptJsonObj.getAsJsonObject("references") != null &&
					conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl") != null &&
					conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl").size() > 0) {

				JsonArray prefLabelXlArray = conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl");
				for(JsonElement prefLabelXlElem : prefLabelXlArray) {
					JsonObject prefLabelXlObj = prefLabelXlElem.getAsJsonObject();
					setLabelFromConceptJson(prefLabelXlObj, outputLabelObj);
				}
			}

			if(conceptJsonObj.getAsJsonObject("references") != null &&
					conceptJsonObj.getAsJsonObject("references").getAsJsonArray("altLabelXl") != null &&
					conceptJsonObj.getAsJsonObject("references").getAsJsonArray("altLabelXl").size() > 0) {

				JsonArray altLabelXlArray = conceptJsonObj.getAsJsonObject("references").getAsJsonArray("altLabelXl");
				for(JsonElement altLabelXlElem : altLabelXlArray) {
					JsonObject altLabelXlObj = altLabelXlElem.getAsJsonObject();
					if(altLabelXlObj.getAsJsonObject("properties") != null &&
							altLabelXlObj.getAsJsonObject("properties").getAsJsonArray("prefLabel") != null &&
							altLabelXlObj.getAsJsonObject("properties").getAsJsonArray("prefLabel").size() > 0) {

						JsonArray prefLabelArrayInAltLabelXl = altLabelXlObj.getAsJsonObject("properties").getAsJsonArray("prefLabel");
						for(JsonElement prefLabelInAltLabelXlElem : prefLabelArrayInAltLabelXl) {
							JsonObject prefLabelInAltLabelXlObj = prefLabelInAltLabelXlElem.getAsJsonObject();
							JsonObject altLabelObjInOutput = output.getAsJsonObject("altLabel");
							if(altLabelObjInOutput.getAsJsonArray(prefLabelInAltLabelXlObj.get("lang").getAsString()) == null) {
								altLabelObjInOutput.add(prefLabelInAltLabelXlObj.get("lang").getAsString(), new JsonArray());
							}
							JsonArray altLabelInLangArrayOutput = altLabelObjInOutput.getAsJsonArray(prefLabelInAltLabelXlObj.get("lang").getAsString());
							altLabelInLangArrayOutput.add(prefLabelInAltLabelXlObj.get("value").getAsString());
						}
					}
				}
			}
			return output;
		}
		log.warn("Unable to transform JSON from termed API to JSON required by elasticsearch for " + conceptJsonObj.get("id").getAsString());
		return null;
	}

	private void setLabelFromConceptJson(JsonObject conceptObj, JsonObject outputObj) {
		if(conceptObj.getAsJsonObject("properties") != null &&
				conceptObj.getAsJsonObject("properties").getAsJsonArray("prefLabel") != null &&
				conceptObj.getAsJsonObject("properties").getAsJsonArray("prefLabel").size() > 0) {

			JsonArray prefLabelArray = conceptObj.getAsJsonObject("properties").getAsJsonArray("prefLabel");
			for(JsonElement prefLabelElem : prefLabelArray) {
				JsonObject prefLabelObj = prefLabelElem.getAsJsonObject();
				outputObj.addProperty(prefLabelObj.get("lang").getAsString(), prefLabelObj.get("value").getAsString());
			}
		}
	}

	private boolean isValidJsonForIndex(JsonObject conceptJsonObj) {

		if(conceptJsonObj.get("id") == null){
			return false;
		}

		if(	conceptJsonObj.getAsJsonObject("type") == null ||
			conceptJsonObj.getAsJsonObject("type").getAsJsonObject("graph") == null ||
			conceptJsonObj.getAsJsonObject("type").getAsJsonObject("graph").get("id") == null) {
			return false;
		}

		if( (	conceptJsonObj.getAsJsonObject("properties") != null &&
				conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("prefLabel") != null &&
				conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("prefLabel").size() > 0
			)
		||
			(	conceptJsonObj.getAsJsonObject("references") != null &&
				conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl") != null &&
				conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl").size() > 0

			)
		) {
			return true;
		}
		return false;
	}
}
