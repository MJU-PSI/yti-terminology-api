package fi.csc.termed.search.service.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.csc.termed.search.service.json.JsonTools;
import fi.csc.termed.search.service.json.TermedExtJsonService;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.List;

/**
 * Created by jmlehtin on 28/3/2017.
 */

@Service
public class TermedExtApiService extends ApiTools {

	@Value("${api.user}")
	private String API_USER;

	@Value("${api.pw}")
	private String API_PW;

	@Value("${api.host.url}")
	private String API_HOST_URL;

	@Value("${api.concept.get.one.urlContext}")
	private String GET_ONE_CONCEPT_URL_CONTEXT;

	@Value("${api.vocabulary.get.one.urlContext}")
	private String GET_ONE_VOCABULARY_URL_CONTEXT;

	private HttpClient apiClient;

	private TermedExtJsonService termedExtJsonService;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	public TermedExtApiService(TermedExtJsonService termedExtJsonService) {
		this.termedExtJsonService = termedExtJsonService;
		this.apiClient = HttpClientBuilder.create().build();
	}

	public JsonObject fetchConcept(String vocabularyId, String conceptId) {
		if(vocabularyId != null && conceptId != null) {
			return fetchJsonObjectFromUrl(API_HOST_URL + MessageFormat.format(GET_ONE_CONCEPT_URL_CONTEXT, vocabularyId, conceptId));
		}
		log.error("VocabularyId or conceptId not supplied for fetching concept data from termed API");
		return null;
	}

	public JsonElement getVocabularyForIndexing(String vocabularyId) {
		JsonObject vocOutputObj = new JsonObject();
		JsonObject vocJsonObj = fetchVocabulary(vocabularyId);

		if (vocJsonObj != null && JsonTools.isValidVocabularyJsonForIndex(vocJsonObj)) {
			vocOutputObj.addProperty("id", vocabularyId);
			JsonObject labelObj = new JsonObject();
			vocOutputObj.add("label", labelObj);

			if(JsonTools.setLabelsFromJson(vocJsonObj, labelObj)) {
				return vocOutputObj;
			} else {
				log.error("Unable to transform vocabulary JSON for indexing");
			}
		} else {
			log.error("Unable to create vocabulary JSON");
		}
		return null;
	}

	private JsonObject fetchVocabulary(String vocabularyId) {
		if(vocabularyId != null) {
			List<JsonObject> retObj = fetchJsonObjectsInArrayFromUrl(API_HOST_URL + MessageFormat.format(GET_ONE_VOCABULARY_URL_CONTEXT, vocabularyId, VocabularyType.TerminologicalVocabulary.name()));
			if(retObj == null) {
				log.info("Vocabulary " + vocabularyId + " was not found as type " + VocabularyType.TerminologicalVocabulary.name() + ". Trying to find as type " + VocabularyType.Vocabulary.name());
				retObj = fetchJsonObjectsInArrayFromUrl(API_HOST_URL + MessageFormat.format(GET_ONE_VOCABULARY_URL_CONTEXT, vocabularyId, VocabularyType.Vocabulary.name()));
			}
			return retObj.get(0);
		}
		log.error("GraphId not supplied for fetching vocabulary data from termed API");
		return null;
	}

}
