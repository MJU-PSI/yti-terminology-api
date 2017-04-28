package fi.csc.termed.search.service.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.csc.termed.search.service.json.JsonTools;
import fi.csc.termed.search.service.json.TermedExtJsonService;
import fi.csc.termed.search.service.json.TermedJsonService;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.*;

/**
 * Created by jmlehtin on 28/3/2017.
 */

@Service
public class TermedApiService extends ApiTools {

	@Value("${api.user}")
	private String API_USER;

	@Value("${api.pw}")
	private String API_PW;

	@Value("${api.host.url}")
	private String API_HOST_URL;

	@Value("${api.vocabulary.get.all.urlContext}")
	private String GET_ALL_VOCABULARIES_URL_CONTEXT;

	@Value("${api.vocabulary.nodes.get.all.urlContext}")
	private String GET_ALL_NODES_IN_VOCABULARY_URL_CONTEXT;

	@Value("${api.eventHook.register.urlContext}")
	private String API_REGISTER_LISTENER_URL_CONTEXT;

	@Value("${api.eventHook.delete.urlContext}")
	private String API_DELETE_LISTENER_URL_CONTEXT;

	private HttpClient apiClient;

	private TermedExtJsonService termedExtJsonService;
	private TermedJsonService termedJsonService;
	private JsonParser gsonParser;

	private static Map<String, JsonElement> vocabularyCache = new HashMap<>();

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	public TermedApiService(TermedExtJsonService termedExtJsonService, TermedJsonService termedJsonService) {
		this.termedExtJsonService = termedExtJsonService;
		this.termedJsonService = termedJsonService;
		this.apiClient = HttpClientBuilder.create().build();
		this.gsonParser = new JsonParser();
	}

	public boolean deleteChangeListener(String hookId) {
		HttpDelete deleteReq = new HttpDelete(API_HOST_URL + MessageFormat.format(API_DELETE_LISTENER_URL_CONTEXT, hookId));
		deleteReq.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
		try {
			HttpResponse resp = apiClient.execute(deleteReq);
			if(resp.getStatusLine().getStatusCode() < 200 || resp.getStatusLine().getStatusCode() >= 400) {
				log.error("Response code: " + resp.getStatusLine().getStatusCode());
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			deleteReq.releaseConnection();
		}
		return true;
	}

	public String registerChangeListener() {
		HttpPost registerReq = new HttpPost(API_HOST_URL + API_REGISTER_LISTENER_URL_CONTEXT);
		registerReq.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
		try {
			HttpResponse resp = apiClient.execute(registerReq);
			if(resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 400) {
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(resp.getEntity().getContent()));

				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				return result.toString().replace("<string>", "").replace("</string>", "");
			} else {
				log.error("Response code: " + resp.getStatusLine().getStatusCode());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			registerReq.releaseConnection();
		}
		return null;
	}

	public List<String> fetchAllAvailableVocabularyIds() {
		log.info("Fetching all vocabulary IDs..");
		return JsonTools.getIdsFromArrayJsonObjects(fetchJsonObjectsInArrayFromUrl(API_HOST_URL + GET_ALL_VOCABULARIES_URL_CONTEXT));
	}

	public Optional<JsonObject> getOneVocabulary(String vocabularyId, List<JsonObject> nodes) {
		return nodes.stream().
				filter(obj -> obj.get("id") != null && termedJsonService.isTerminologicalVocabularyNode(obj) && obj.get("type").getAsJsonObject().get("graph").getAsJsonObject().get("id").getAsString().equals(vocabularyId)).findFirst();
	}

	public List<JsonObject> fetchAllNodesInVocabulary(String vocabularyId) {
		List<JsonObject> allNodes = fetchJsonObjectsInArrayFromUrl(API_HOST_URL + MessageFormat.format(GET_ALL_NODES_IN_VOCABULARY_URL_CONTEXT, vocabularyId));
		if(allNodes == null) {
			return new ArrayList<>();
		}
		return allNodes;

	}

	public Map<String, JsonObject> getAllConceptsFromNodes(List<JsonObject> nodes) {
		Map<String, JsonObject> retVal = new HashMap<>();
		nodes.stream().filter(obj -> obj.get("id") != null && termedJsonService.isConceptNode(obj))
				.forEach(concept -> retVal.put(concept.get("id").getAsString(), concept));
		return retVal;
	}

	public Map<String, JsonObject> getAllTermsFromNodes(List<JsonObject> nodes) {
		Map<String, JsonObject> retVal = new HashMap<>();
		nodes.stream().filter(obj -> obj.get("id") != null && termedJsonService.isTermNode(obj))
				.forEach(concept -> retVal.put(concept.get("id").getAsString(), concept));
		return retVal;
	}

	public JsonElement transformVocabularyForIndexing(JsonObject vocabularyJsonObj) {
		JsonObject vocOutputObj = new JsonObject();

		if (vocabularyJsonObj != null && JsonTools.isValidVocabularyJsonForIndex(vocabularyJsonObj)) {
			vocOutputObj.addProperty("id", vocabularyJsonObj.get("type").getAsJsonObject().get("graph").getAsJsonObject().get("id").getAsString());
			JsonObject labelObj = new JsonObject();
			vocOutputObj.add("label", labelObj);

			if(JsonTools.setLabelsFromJson(vocabularyJsonObj, labelObj)) {
				return vocOutputObj;
			} else {
				log.error("Unable to transform vocabulary JSON for indexing");
			}
		} else {
			log.error("Unable to create vocabulary JSON");
		}
		return null;
	}

}
