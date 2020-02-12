package fi.vm.yti.terminology.api.system;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.util.Parameters;
import static org.springframework.http.HttpMethod.GET;

@Service
public class SystemService {

    private static final Logger logger = LoggerFactory.getLogger(SystemService.class);
    private final TermedRequester termedRequester;
    private final ObjectMapper objectMapper;

    @Autowired
    public SystemService(final TermedRequester termedRequester,
                         final ObjectMapper objectMapper) {
        this.termedRequester = termedRequester;
        this.objectMapper = objectMapper;
    }

    ResponseEntity<String> countStatistics(boolean full) throws IOException {

        int terminologies = countTerminologies();
        int concepts = countConcepts();

        if (full) {
            String terminologyStatistics = countStatistics();
            String response = "{\n  \"terminologyCount\": " + terminologies + ",\n  \"conceptCount\": " + concepts
                + ",\n  \"statistics\": [\n" + terminologyStatistics + "\n  ]\n}";
            logger.debug("countStatistics response: " + response);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            String response = "{\n  \"terminologyCount\": " + terminologies + ",\n  \"conceptCount\": " + concepts + "\n}";
            logger.debug("countStatistics response: " + response);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    private int countTerminologies() {
        int rv = 0;
        String url = "/node-count?where=type.id:TerminologicalVocabulary";
        String count = termedRequester.exchange(url, GET, Parameters.empty(), String.class);
        if (count != null) {
            rv = Integer.parseInt(count);
        }
        return rv;
    }

    private int countConcepts() {
        int rv = 0;
        String url = "/node-count?where=type.id:Concept";
        String count = termedRequester.exchange(url, GET, Parameters.empty(), String.class);
        if (count != null) {
            rv = Integer.parseInt(count);
        }
        return rv;
    }

    private int countConcepts(String terminologyId) {
        int rv = 0;
        String url = "/graphs/" + terminologyId + "/types/Concept/node-count";
        String count = termedRequester.exchange(url, GET, Parameters.empty(), String.class);
        if (count != null) {
            rv = Integer.parseInt(count);
        }
        return rv;
    }

    private String countStatistics() throws IOException {
        String rv = null;
        List<String> statistics = new ArrayList<>();
        String url = "/node-trees?select=uri,type&where=type.id:TerminologicalVocabulary";
        String terminologies = termedRequester.exchange(url, GET, Parameters.empty(), String.class);
        if (terminologies != null) {
            Terminology[] tnodes = objectMapper.readValue(terminologies, Terminology[].class);
            for (Terminology t : tnodes) {
                statistics.add("    { \"uri\": " + "\"" + t.getUri() + "\", \"count\": " + countConcepts(t.getType().getGraph().getId()) + " }");
            }
            rv = statistics.stream().collect(Collectors.joining(",\n"));
        }
        return rv;
    }

    private static class Graph {

        private String id;

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }
    }

    private static class Type {

        private Graph graph;
        private String id;

        public Graph getGraph() {
            return graph;
        }

        public void setGraph(final Graph graph) {
            this.graph = graph;
        }

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }
    }

    private static class Terminology {

        private String uri;
        private Type type;

        public String getUri() {
            return uri;
        }

        public void setUri(final String uri) {
            this.uri = uri;
        }

        public Type getType() {
            return type;
        }

        public void setType(final Type type) {
            this.type = type;
        }
    }
}
