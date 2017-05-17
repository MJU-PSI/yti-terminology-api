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
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
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
import java.util.*;
import java.util.stream.Collectors;

import static fi.csc.termed.search.util.JsonUtils.asStream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Service
public class TermedApiService {

	@Value("${api.user}")
	private String API_USER;

	@Value("${api.pw}")
	private String API_PW;

	@Value("${api.url}")
	private String API_URL;

    private final HttpClient apiClient;
    private final JsonParser jsonParser;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public TermedApiService() {
		this.apiClient = HttpClientBuilder.create().build();
		this.jsonParser = new JsonParser();
	}

	public boolean deleteChangeListener(@NotNull String hookId) {
		HttpDelete deleteReq = new HttpDelete(createUrl("/hooks/" + hookId));
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

	public @Nullable String registerChangeListener(String url) {
		HttpPost registerReq = new HttpPost(createUrl("/hooks", Parameters.single("url", url)));
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

		return fetchJsonObjectsInArrayFromUrl(createUrl("/graphs")).stream()
                .map(x -> x.get("id").getAsString())
                .collect(toList());
	}

	@NotNull List<Concept> getAllConceptsForGraph(@NotNull String graphId) {

	    AllNodesResult allNodesResult = this.fetchAllNodesInGraph(graphId);

        Optional<String> vocabularyNodeId = allNodesResult.getVocabularyNodeId();

        if (vocabularyNodeId.isPresent()) {
            return allNodesResult.getConceptNodeIds().stream()
                    .map(conceptId -> Concept.createFromAllNodeResult(conceptId, vocabularyNodeId.get(), allNodesResult))
                    .collect(toList());
        } else {
            log.warn("Vocabulary not found for graph: " + graphId);
            return emptyList();
        }
	}

    @NotNull List<Concept> getConcepts(@NotNull String graphId, @NotNull Collection<String> ids) {

        Vocabulary vocabulary = getVocabulary(graphId);

        if (vocabulary != null) {
            return ids.stream()
                    .map(id -> getConcept(graphId, id))
                    .filter(Objects::nonNull)
                    .collect(toList());
        } else {
            return emptyList();
        }
    }

    @Nullable Concept getConcept(@NotNull String graphId, @NotNull String conceptId) {

        Vocabulary vocabulary = getVocabulary(graphId);

        if (vocabulary != null) {
            return getConcept(conceptId, vocabulary);
        } else {
            return null;
        }
    }

    private @Nullable Concept getConcept(@NotNull String conceptId, @NotNull Vocabulary vocabulary) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "code");
        params.add("select", "uri");
        params.add("select", "createdBy");
        params.add("select", "createdDate");
        params.add("select", "lastModifiedBy");
        params.add("select", "lastModifiedDate");
        params.add("select", "properties.prefLabel");
        params.add("select", "properties.definition");
        params.add("select", "properties.status");
        params.add("select", "references.prefLabelXl:2");
        params.add("select", "references.altLabelXl:2");
        params.add("select", "references.broader");
        params.add("select", "referrers.broader");
        params.add("where", "graph.id:" + vocabulary.getGraphId());
        params.add("where", "id:" + conceptId);
        params.add("max", "-1");
        
	    JsonObject result = single(fetchJsonObjectsInArrayFromUrl(createUrl("/node-trees", params)));

        if (result != null) {
            return Concept.createFromExtJson(result, vocabulary);
        } else {
            log.warn("Concept not found: " + conceptId);
            return null;
        }
    }

    private @Nullable Vocabulary getVocabulary(@NotNull String graphId) {

	    JsonObject vocabularyNode = getVocabularyNode(graphId);

        if (vocabularyNode != null) {
            return Vocabulary.createFromExtJson(vocabularyNode);
        } else {
            log.warn("Vocabulary not found for graph " + graphId);
            return null;
        }
    }

    private @Nullable JsonObject getVocabularyNode(@NotNull String graphId) {

        JsonObject json = getVocabularyNode(graphId, VocabularyType.TerminologicalVocabulary);

        if (json != null) {
            return json;
        } else {
            log.info("Vocabulary for graph " + graphId + " was not found as type " + VocabularyType.TerminologicalVocabulary.name() + ". Trying to find as type " + VocabularyType.Vocabulary.name());
            return getVocabularyNode(graphId, VocabularyType.Vocabulary);
        }
    }

    private @Nullable JsonObject getVocabularyNode(@NotNull String graphId, @NotNull VocabularyType vocabularyType) {

        Parameters params = new Parameters();
        params.add("select", "id");
        params.add("select", "type");
        params.add("select", "properties.*");
        params.add("where", "graph.id:" + graphId);
        params.add("where", "type.id:" + vocabularyType.name());
        params.add("max", "-1");

        return single(fetchJsonObjectsInArrayFromUrl(createUrl("/node-trees", params)));
    }

    private @NotNull AllNodesResult fetchAllNodesInGraph(String graphId) {
        return new AllNodesResult(fetchJsonObjectsInArrayFromUrl(createUrl("/graphs/" + graphId + "/nodes", Parameters.single("max", "-1"))));
    }

    private @NotNull List<JsonObject> fetchJsonObjectsInArrayFromUrl(@NotNull String url) {

	    HttpGet getObjectsReq = new HttpGet(url);

        try {
            getObjectsReq.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
            HttpResponse resp = apiClient.execute(getObjectsReq);
            if (resp.getStatusLine().getStatusCode() == 200) {
                JsonArray docs = jsonParser.parse(EntityUtils.toString(resp.getEntity())).getAsJsonArray();
                List<JsonObject> result = asStream(docs).map(JsonElement::getAsJsonObject).collect(toList());
                log.info("Fetched " + result.size() + " objects from termed API from url " + url);
                return result;
            } else {
                log.warn("Fetching objects failed with code: " + resp.getStatusLine().getStatusCode());
                return emptyList();
            }
        } catch (IOException e) {
            log.error("Fetching objects failed");
            e.printStackTrace();
            return emptyList();
        } finally {
            getObjectsReq.releaseConnection();
        }
    }

    private @NotNull String getAuthHeader() {
        return "Basic " + Base64.getEncoder().encodeToString((API_USER + ":" + API_PW).getBytes());
    }

    private static @Nullable JsonObject single(@NotNull List<JsonObject> objects) {
        if (objects.size() == 0) {
            return null;
        } else {
            return objects.get(0);
        }
    }

    private @NotNull String createUrl(@NotNull String path) {
        return createUrl(path, new Parameters());
    }

    private @NotNull String createUrl(@NotNull String path, @NotNull Parameters parameters) {
        return API_URL + path + parameters.toString();
    }

    private static class Parameters {
        
	    private final List<NameValuePair> parameters = new ArrayList<>();

	    private static @NotNull Parameters single(@NotNull String name, @NotNull String value) {
            Parameters result = new Parameters();
            result.add(name, value);
            return result;
        }
	    
        private void add(@NotNull String name, @NotNull String value) {
            this.parameters.add(new BasicNameValuePair(name, value));
        }

        @Override
        public String toString() {

            StringBuilder result = new StringBuilder();

            if (!parameters.isEmpty()) {
                result.append("?");
                result.append(
                        parameters.stream()
                                .map(param -> param.getName() + "=" + param.getValue())
                                .collect(Collectors.joining("&")));
            }

            return result.toString();
        }
    }
}