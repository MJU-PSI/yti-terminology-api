package fi.csc.termed.api.index;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.csc.termed.api.util.JsonUtils;
import fi.csc.termed.api.util.Parameters;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Service
public class TermedService {

	@Value("${api.user}")
	private String API_USER;

	@Value("${api.pw}")
	private String API_PW;

	@Value("${api.url}")
	private String API_URL;

    private final HttpClient apiClient;
    private final JsonParser jsonParser;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public TermedService() {
		this.apiClient = HttpClientBuilder.create().build();
		this.jsonParser = new JsonParser();
	}

	public boolean deleteChangeListener(@NotNull String hookId) {
        return executeRequest(new HttpDelete(createUrl("/hooks/" + hookId))) != null;
	}

	public @Nullable String registerChangeListener(@NotNull String url) {

	    HttpPost request = new HttpPost(createUrl("/hooks", Parameters.single("url", url)));
        HttpEntity response = executeRequest(request);

        if (response != null) {
            return responseContentAsString(response)
                    .replace("<string>", "")
                    .replace("</string>", "");
        } else {
            return null;
        }
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

	    if (ids.isEmpty()) {
	        return emptyList();
        }

        Vocabulary vocabulary = getVocabulary(graphId);

        if (vocabulary != null) {
            return getConcepts(vocabulary, ids);
        } else {
            return emptyList();
        }
    }

    private @NotNull List<Concept> getConcepts(@NotNull Vocabulary vocabulary, @NotNull Collection<String> conceptIds) {

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
        params.add("where", idInCollectionWhereClause(conceptIds));
        params.add("max", "-1");
        
	    return fetchJsonObjectsInArrayFromUrl(createUrl("/node-trees", params))
                .stream()
                .map(json -> Concept.createFromExtJson(json, vocabulary))
                .collect(toList());
    }

    private static @NotNull String idInCollectionWhereClause(@NotNull Collection<String> conceptIds) {

	    return conceptIds.stream()
                .map(conceptId -> "id:" + conceptId)
                .collect(Collectors.joining(" OR "));
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

        HttpEntity response = executeRequest(new HttpGet(url));

        if (response != null) {

            JsonArray json = responseContentAsJson(response).getAsJsonArray();

            log.info("Fetched " + json.size() + " objects from termed API from url " + url);
            return JsonUtils.asStream(json).map(JsonElement::getAsJsonObject).collect(toList());
        } else {
            return emptyList();
        }
    }

    private @NotNull JsonElement responseContentAsJson(@NotNull HttpEntity response) {
        return jsonParser.parse(responseContentAsString(response));
    }

    private static @NotNull String responseContentAsString(@NotNull HttpEntity response) {
        try (InputStream is = response.getContent()) {
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private @Nullable HttpEntity executeRequest(@NotNull HttpRequestBase request) {

        request.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());

        try {
            HttpResponse response = apiClient.execute(request);

            if(isSuccess(response)) {
                return response.getEntity();
            } else {
                log.warn("Response code: " + response.getStatusLine().getStatusCode() + " for request " + request.toString());
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean isSuccess(@NotNull HttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        return statusCode >= 200 && statusCode < 400;
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
        return API_URL + path + parameters.toEncodedString();
    }
}