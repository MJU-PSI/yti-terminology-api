package fi.csc.termed.search.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
public class TermedApiService {

	@Value("${api.user}")
	private String API_USER;

	@Value("${api.pw}")
	private String API_PW;

	@Value("${api.host.url}")
	private String API_HOST_URL;

	@Value("${api.concept.get.all.urlContext}")
	private String GET_ALL_CONCEPTS_URL_CONTEXT;

	@Value("${api.concept.get.one.urlContext}")
	private String GET_ONE_CONCEPT_URL_CONTEXT;

	@Value("${api.vocabulary.get.one.urlContext}")
	private String GET_ONE_VOCABULARY_URL_CONTEXT;

	@Value("${api.vocabulary.concept.get.all.urlContext}")
	private String GET_ALL_CONCEPTS_IN_VOCABULARY_URL_CONTEXT;

	@Value("${api.eventHook.register.urlContext}")
	private String API_REGISTER_LISTENER_URL_CONTEXT;

	@Value("${api.eventHook.delete.urlContext}")
	private String API_DELETE_LISTENER_URL_CONTEXT;

	private HttpClient apiClient;

	private JsonParserService jsonParserService;

	private static Map<String, JsonElement> vocabularyCache = new HashMap<>();

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	public TermedApiService(JsonParserService jsonParserService) {
		this.jsonParserService = jsonParserService;
		this.apiClient = HttpClientBuilder.create().build();
	}

	private String getAuthHeader() {
		return "Basic " + new String(Base64.getEncoder().encodeToString((API_USER + ":" + API_PW).getBytes()));
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
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			registerReq.releaseConnection();
		}
	}

	public List<JsonObject> fetchAllConcepts() {
		return fetchJsonObjectsInArrayFromUrl(API_HOST_URL + GET_ALL_CONCEPTS_URL_CONTEXT);
	}

	public List<JsonObject> fetchAllConceptsInVocabulary(String vocabularyId) {
		return fetchJsonObjectsInArrayFromUrl(API_HOST_URL + MessageFormat.format(GET_ALL_CONCEPTS_IN_VOCABULARY_URL_CONTEXT, vocabularyId));
	}

	public JsonObject fetchConcept(String vocabularyId, String conceptId) {
		if(vocabularyId != null && conceptId != null) {
			return fetchJsonObjectFromUrl(API_HOST_URL + MessageFormat.format(GET_ONE_CONCEPT_URL_CONTEXT, vocabularyId, conceptId));
		}
		log.error("VocabularyId or conceptId not supplied for fetching concept data from termed API");
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

	private List<JsonObject> fetchJsonObjectsInArrayFromUrl(String url) {
		List<JsonObject> allObjects = new ArrayList<>();
		HttpGet getObjectsReq = new HttpGet(url);
		try {
			getObjectsReq.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
			HttpResponse resp = apiClient.execute(getObjectsReq);
			if (resp.getStatusLine().getStatusCode() == 200) {
				JsonArray docs = jsonParserService.getJsonParser().parse(EntityUtils.toString(resp.getEntity())).getAsJsonArray();
				Iterator<JsonElement> it = docs.iterator();
				int fetched = 0;
				while (it.hasNext()) {
					JsonElement docElem = it.next();
					if(docElem.isJsonObject()) {
						allObjects.add(docElem.getAsJsonObject());
						fetched++;
					}
				}
				log.info("Fetched " + fetched + " objects from termed API from url " + url);
			} else {
				log.warn("Fetching objects failed with code: " + resp.getStatusLine().getStatusCode());
				return null;
			}
		} catch (IOException e) {
			log.error("Fetching objects failed");
			e.printStackTrace();
			return null;
		} finally {
			getObjectsReq.releaseConnection();
		}
		return allObjects;
	}

	private JsonObject fetchJsonObjectFromUrl(String url) {
		if(url != null) {
			HttpGet getRequest = new HttpGet(url);
			try {
				getRequest.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
				HttpResponse resp = apiClient.execute(getRequest);
				if (resp.getStatusLine().getStatusCode() == 200) {
					String respStr = EntityUtils.toString(resp.getEntity());
					JsonObject obj = jsonParserService.getJsonParser().parse(respStr).getAsJsonObject();
					if(obj != null) {
						return obj;
					} else {
						log.error("Unable to parse response JSON from " + url);
						return null;
					}
				} else {
					log.warn("Fetching JSON from " + url + " failed with code: " + resp.getStatusLine().getStatusCode());
					return null;
				}
			} catch (IOException e) {
				log.error("Fetching JSON failed");
				e.printStackTrace();
				return null;
			} finally {
				getRequest.releaseConnection();
			}
		}
		return null;
	}

	public JsonElement fetchVocabularyForConcept(String vocabularyId, boolean useCache) {
		if(vocabularyId != null) {
			if(useCache) {
				if(vocabularyCache.get(vocabularyId) == null) {
					JsonElement vocObjForIndex = transformVocabularyForIndexing(vocabularyId);
					vocabularyCache.put(vocabularyId, vocObjForIndex);
				}
				return vocabularyCache.get(vocabularyId);
			} else {
				return transformVocabularyForIndexing(vocabularyId);
			}
		} else {
			log.error("Unable to fetch vocabulary for concept");
		}
		return null;
	}

	private JsonElement transformVocabularyForIndexing(String vocabularyId) {
		JsonObject vocOutputObj = new JsonObject();
		JsonObject vocJsonObj = fetchVocabulary(vocabularyId);

		if (vocJsonObj != null && jsonParserService.isValidVocabularyJsonForIndex(vocJsonObj)) {
			vocOutputObj.addProperty("id", vocabularyId);
			JsonObject labelObj = new JsonObject();
			vocOutputObj.add("label", labelObj);

			if(jsonParserService.setLabelsFromJson(vocJsonObj, labelObj)) {
				return vocOutputObj;
			} else {
				log.error("Unable to transform vocabulary JSON for indexing");
			}
		} else {
			log.error("Unable to create vocabulary JSON");
		}
		return null;
	}

	public void invalidateVocabularyCache() {
		TermedApiService.vocabularyCache.clear();
	}

	public enum VocabularyType {
		TerminologicalVocabulary,
		Vocabulary
	}

}
