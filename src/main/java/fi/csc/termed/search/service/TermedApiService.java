package fi.csc.termed.search.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.csc.termed.search.domain.Concept;
import fi.csc.termed.search.domain.Vocabulary;
import fi.csc.termed.search.domain.VocabularyType;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Service
public class TermedApiService {

	@Value("${api.user}")
	private String API_USER;

	@Value("${api.pw}")
	private String API_PW;

	@Value("${api.host.url}")
	private String API_HOST_URL;

	@Value("${api.vocabulary.get.all.urlContext}")
	private String GET_ALL_VOCABULARIES_URL_CONTEXT;

    @Value("${api.vocabulary.get.one.urlContext}")
    private String GET_ONE_VOCABULARY_URL_CONTEXT;

	@Value("${api.vocabulary.nodes.get.all.urlContext}")
	private String GET_ALL_NODES_IN_VOCABULARY_URL_CONTEXT;

	@Value("${api.eventHook.register.urlContext}")
	private String API_REGISTER_LISTENER_URL_CONTEXT;

	@Value("${api.eventHook.delete.urlContext}")
	private String API_DELETE_LISTENER_URL_CONTEXT;

    @Value("${api.concept.get.one.urlContext}")
    private String GET_ONE_CONCEPT_URL_CONTEXT;

    private final HttpClient apiClient;
    private final JsonParser jsonParser;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public TermedApiService() {
		this.apiClient = HttpClientBuilder.create().build();
		this.jsonParser = new JsonParser();
	}

	public boolean deleteChangeListener(@NotNull String hookId) {
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

	public @Nullable String registerChangeListener() {
		HttpPost registerReq = new HttpPost(API_HOST_URL + API_REGISTER_LISTENER_URL_CONTEXT);
		registerReq.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
		try {
			HttpResponse resp = apiClient.execute(registerReq);
			if(resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 400) {
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(resp.getEntity().getContent()));

				StringBuilder result = new StringBuilder();
				String line;
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

	@NotNull List<String> fetchAllAvailableGraphIds() {

	    log.info("Fetching all graph IDs..");

		return fetchJsonObjectsInArrayFromUrl(API_HOST_URL + GET_ALL_VOCABULARIES_URL_CONTEXT).stream()
                .map(x -> x.get("id").getAsString())
                .collect(toList());
	}

	@NotNull List<Concept> getAllConceptsForGraph(String graphId) {

	    AllNodesResult allNodesResult = this.fetchAllNodesInGraph(graphId);

        Optional<String> vocabularyNodeId = allNodesResult.getVocabularyNodeId();

        if (vocabularyNodeId.isPresent()) {
            return allNodesResult.getConceptNodeIds().stream()
                    .map(conceptId -> Concept.createFromAllNodeResult(conceptId, vocabularyNodeId.get(), allNodesResult))
                    .collect(toList());
        } else {
            log.warn("Vocabulary not found for graph: " + graphId);
            return Collections.emptyList();
        }
	}

    @NotNull List<Concept> getConcepts(String graphId, Collection<String> ids) {

        Vocabulary vocabulary = getVocabulary(graphId);

        if (vocabulary != null) {
            return ids.stream()
                    .map(id -> getConcept(graphId, id))
                    .filter(Objects::nonNull)
                    .collect(toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Nullable Concept getConcept(String graphId, String conceptId) {

        Vocabulary vocabulary = getVocabulary(graphId);

        if (vocabulary != null) {
            return getConcept(conceptId, vocabulary);
        } else {
            return null;
        }
    }

    private @Nullable Concept getConcept(String conceptId, Vocabulary vocabulary) {

	    JsonObject jsonObject = fetchJsonObjectFromUrl(API_HOST_URL + MessageFormat.format(GET_ONE_CONCEPT_URL_CONTEXT, vocabulary.getGraphId(), conceptId));

        if (jsonObject != null) {
            return Concept.createFromExtJson(jsonObject, vocabulary);
        } else {
            log.warn("Concept not found: " + conceptId);
            return null;
        }
    }

    private @Nullable Vocabulary getVocabulary(String graphId) {

	    JsonObject vocabularyNode = getVocabularyNode(graphId);

        if (vocabularyNode != null) {
            return Vocabulary.createFromExtJson(vocabularyNode);
        } else {
            log.warn("Vocabulary not found for graph " + graphId);
            return null;
        }
    }

    private @Nullable JsonObject getVocabularyNode(String graphId) {
        if (graphId != null) {
            List<JsonObject> retObj = fetchJsonObjectsInArrayFromUrl(API_HOST_URL + MessageFormat.format(GET_ONE_VOCABULARY_URL_CONTEXT, graphId, VocabularyType.TerminologicalVocabulary.name()));
            if(retObj.size() == 0) {
                log.info("Vocabulary for graph " + graphId + " was not found as type " + VocabularyType.TerminologicalVocabulary.name() + ". Trying to find as type " + VocabularyType.Vocabulary.name());
                retObj = fetchJsonObjectsInArrayFromUrl(API_HOST_URL + MessageFormat.format(GET_ONE_VOCABULARY_URL_CONTEXT, graphId, VocabularyType.Vocabulary.name()));
            }
            return retObj.get(0);
        }
        log.warn("Graph id not supplied for fetching vocabulary data from termed API");
        return null;
    }

    private @NotNull AllNodesResult fetchAllNodesInGraph(String graphId) {
        return new AllNodesResult(fetchJsonObjectsInArrayFromUrl(API_HOST_URL + MessageFormat.format(GET_ALL_NODES_IN_VOCABULARY_URL_CONTEXT, graphId)));
    }

    private @NotNull List<JsonObject> fetchJsonObjectsInArrayFromUrl(String url) {

        List<JsonObject> allObjects = new ArrayList<>();
        HttpGet getObjectsReq = new HttpGet(url);
        try {
            getObjectsReq.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
            HttpResponse resp = apiClient.execute(getObjectsReq);
            if (resp.getStatusLine().getStatusCode() == 200) {
                JsonArray docs = jsonParser.parse(EntityUtils.toString(resp.getEntity())).getAsJsonArray();
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
                return allObjects;
            }
        } catch (IOException e) {
            log.error("Fetching objects failed");
            e.printStackTrace();
            return allObjects;
        } finally {
            getObjectsReq.releaseConnection();
        }
        return allObjects;
    }

    private @Nullable JsonObject fetchJsonObjectFromUrl(String url) {
        if(url != null) {
            HttpGet getRequest = new HttpGet(url);
            try {
                getRequest.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
                HttpResponse resp = apiClient.execute(getRequest);
                if (resp.getStatusLine().getStatusCode() == 200) {
                    String respStr = EntityUtils.toString(resp.getEntity());
                    JsonObject obj = jsonParser.parse(respStr).getAsJsonObject();
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

    private @NotNull String getAuthHeader() {
        return "Basic " + Base64.getEncoder().encodeToString((API_USER + ":" + API_PW).getBytes());
    }
}
