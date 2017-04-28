package fi.csc.termed.search.service.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static fi.csc.termed.search.service.json.JsonTools.*;

/**
 * Created by jmlehtin on 28/3/2017.
 */

@Service
public class TermedJsonService  {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public List<JsonObject> transformApiConceptsToIndexConcepts(JsonElement vocabularyJsonObj, Map<String, JsonObject> conceptJsonList, Map<String, JsonObject> termJsonList) {
		if(vocabularyJsonObj == null || conceptJsonList == null || conceptJsonList.size() == 0 || termJsonList == null) {
			log.error("Unable to transform API concept to index document due to missing data");
			return null;
		}

		List<JsonObject> indexConcepts = new ArrayList<>();

		for(Map.Entry<String, JsonObject> conceptEntry : conceptJsonList.entrySet()) {
			JsonObject conceptJsonObj = conceptEntry.getValue();

			if (isValidConceptJsonForIndex(conceptJsonObj)) {
				JsonObject output = new JsonObject();
				JsonArray outputBroaderArray = new JsonArray();
				JsonArray outputNarrowerArray = new JsonArray();
				JsonObject outputDefinitionObj = new JsonObject();
				JsonObject outputLabelObj = new JsonObject();
				JsonObject outputAltLabelObj = new JsonObject();
				output.add("broader", outputBroaderArray);
				output.add("narrower", outputNarrowerArray);
				output.add("definition", outputDefinitionObj);
				output.add("label", outputLabelObj);
				output.add("altLabel", outputAltLabelObj);

				output.addProperty("id", conceptJsonObj.get("id").getAsString());

				if (!isEmptyAsString(conceptJsonObj.get("lastModifiedDate"))) {
					output.addProperty("modified", conceptJsonObj.get("lastModifiedDate").getAsString());
				}


				getBroaderIds(conceptJsonObj).forEach(bId -> outputBroaderArray.add(bId));
				getNarrowerIds(conceptJsonObj).forEach(nId -> outputNarrowerArray.add(nId));

				if (outputNarrowerArray.size() > 0) {
					output.addProperty("hasNarrower", "true");
				} else {
					output.addProperty("hasNarrower", "false");
				}

				if (!isEmptyAsObject(conceptJsonObj.get("properties")) &&
						!isEmptyAsArray(conceptJsonObj.getAsJsonObject("properties").get("status"))) {

					output.addProperty("status", conceptJsonObj.getAsJsonObject("properties").getAsJsonArray("status").get(0).getAsJsonObject().get("value").getAsString());
				}

				output.add("vocabulary", vocabularyJsonObj);

				setDefinition(conceptJsonObj, outputDefinitionObj);

				boolean labelExists = setLabelsFromJson(conceptJsonObj, outputLabelObj);

				if (!labelExists &&
						!isEmptyAsObject(conceptJsonObj.get("references")) &&
						!isEmptyAsArray(conceptJsonObj.getAsJsonObject("references").get("prefLabelXl"))) {

					JsonArray prefLabelXlArray = conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl");
					for (JsonElement prefLabelXlElem : prefLabelXlArray) {
						if (!isEmptyAsObject(prefLabelXlElem) && !isEmptyAsString(prefLabelXlElem.getAsJsonObject().get("id"))) {
							setLabelsFromJson(termJsonList.get(prefLabelXlElem.getAsJsonObject().get("id").getAsString()), outputLabelObj);
						}
					}
				}

				if (!isEmptyAsObject(conceptJsonObj.get("references")) &&
						!isEmptyAsArray(conceptJsonObj.getAsJsonObject("references").get("altLabelXl"))) {

					JsonArray altLabelXlArray = conceptJsonObj.getAsJsonObject("references").getAsJsonArray("altLabelXl");
					for (JsonElement altLabelXlElem : altLabelXlArray) {
						if (!isEmptyAsObject(altLabelXlElem) &&
								!isEmptyAsString(altLabelXlElem.getAsJsonObject().get("id"))) {


							JsonObject altLabelObj = termJsonList.get(altLabelXlElem.getAsJsonObject().get("id").getAsString());
							JsonObject altLabelObjInOutput = output.getAsJsonObject("altLabel");

							if(	!isEmptyAsObject(altLabelObj.get("properties")) &&
									!isEmptyAsArray(altLabelObj.getAsJsonObject("properties").get("prefLabel"))) {

								JsonArray prefLabelArray = altLabelObj.getAsJsonObject("properties").getAsJsonArray("prefLabel");
								setAltLabelsFromPrefLabelArray(prefLabelArray, altLabelObjInOutput);
							}
						}
					}
				}
				indexConcepts.add(output);
			}
		}
		return indexConcepts;
	}

	private List<String> getBroaderIds(JsonObject conceptJsonObj) {
		List<String> broaderIds = new ArrayList<>();
		if (!isEmptyAsObject(conceptJsonObj.get("references")) && !isEmptyAsArray(conceptJsonObj.getAsJsonObject("references").get("broader"))) {
			JsonArray broaderArray = conceptJsonObj.getAsJsonObject("references").getAsJsonArray("broader");
			for(JsonElement broaderObj : broaderArray) {
				if(broaderObj.getAsJsonObject().get("id") != null) {
					broaderIds.add(broaderObj.getAsJsonObject().get("id").getAsString());
				}
			}
		}
		return broaderIds;
	}

	private List<String> getNarrowerIds(JsonObject conceptJsonObj) {
		List<String> narrowerIds = new ArrayList<>();
		if (!isEmptyAsObject(conceptJsonObj.get("referrers")) && !isEmptyAsArray(conceptJsonObj.getAsJsonObject("referrers").get("broader"))) {
			JsonArray narrowerArray = conceptJsonObj.getAsJsonObject("referrers").getAsJsonArray("broader");
			for(JsonElement narrowerObj : narrowerArray) {
				if(narrowerObj.getAsJsonObject().get("id") != null) {
					narrowerIds.add(narrowerObj.getAsJsonObject().get("id").getAsString());
				}
			}
		}
		return narrowerIds;
	}

	public boolean isValidConceptJsonForIndex(JsonObject conceptJsonObj) {
		if(!hasValidId((conceptJsonObj))) {
			return false;
		}

		if(!hasValidGraphId(conceptJsonObj)) {
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
						isEmptyAsString(conceptJsonObj.getAsJsonObject("references").getAsJsonArray("prefLabelXl").get(0).getAsJsonObject().get("id"))
				)
			) {
			return true;
		}
		return false;
	}

	public boolean isConceptNode(JsonObject jsonObj) {
		if(hasTypeId(jsonObj)) {
			return jsonObj.get("type").getAsJsonObject().get("id").getAsString().equals("Concept");
		}
		return false;
	}

	public boolean isTerminologicalVocabularyNode(JsonObject jsonObj) {
		if(hasValidGraphId(jsonObj)) {
			return jsonObj.get("type").getAsJsonObject().get("id").getAsString().equals("TerminologicalVocabulary");
		}
		return false;
	}

	public boolean isTermNode(JsonObject jsonObj) {
		if(hasTypeId(jsonObj)) {
			return jsonObj.get("type").getAsJsonObject().get("id").getAsString().equals("Term");
		}
		return false;
	}

	private boolean hasTypeId(JsonObject jsonObj) {
		return jsonObj.get("type") != null && jsonObj.get("type").getAsJsonObject().get("id") != null;
	}

}