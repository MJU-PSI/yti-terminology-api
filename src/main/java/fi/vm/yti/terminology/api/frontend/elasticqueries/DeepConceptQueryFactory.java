package fi.vm.yti.terminology.api.frontend.elasticqueries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.terminology.api.frontend.searchdto.DeepSearchHitListDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.ConceptSimpleDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.DeepSearchConceptHitListDTO;
import fi.vm.yti.terminology.api.util.ElasticRequestUtils;

public class DeepConceptQueryFactory {

    private static final Logger log = LoggerFactory.getLogger(DeepConceptQueryFactory.class);

    private static final String part0 =
        "{\n" +
            "  \"query\" : {\n" +
            "    \"bool\" : {\n" +
            "      \"must\": [ {\n" +
            "        \"multi_match\" : { \n" +
            "          \"query\" : \"";
    private static final String part1 =
        "\",\n" +
            "          \"fields\" : [ \"label.fi^10\", \"label.*\" ],\n" +
            "          \"type\" : \"best_fields\",\n" +
            "          \"minimum_should_match\" : \"90%\"\n" +
            "        }\n" +
            "      } ],\n" +
            "      \"must_not\" : []\n" +
            "    }\n" +
            "  },\n" +
            "  \"size\" : 0,\n" +
            "  \"aggs\" : {\n" +
            "    \"group_by_terminology\" : {\n" +
            "    \"terms\" : {\n" +
            "      \"field\" : \"vocabulary.id\",\n" +
            "      \"size\" : 1000,\n" +
            "      \"order\" : { \"top_hit\" : \"desc\" }\n" +
            "      },\n" +
            "      \"aggs\" : {\n" +
            "        \"top_terminology_hits\" : {\n" +
            "          \"top_hits\" : {\n" +
            "            \"sort\" : [ { \"_score\" : { \"order\" : \"desc\" } } ],\n" +
            "            \"size\" : 6,\n" +
            "            \"_source\" : {\n" +
            "              \"includes\" : [ \"id\", \"uri\", \"status\", \"label\", \"vocabulary\" ]\n" +
            "            }\n" +
            "          }\n" +
            "        },\n" +
            "        \"top_hit\" : { \"max\" : { \"script\" : { \"source\" : \"_score\" } } }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    private ObjectMapper objectMapper;

    public DeepConceptQueryFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String createQuery(String query) {
        StringBuilder sb = new StringBuilder(part0);
        JsonStringEncoder.getInstance().quoteAsString(query, sb);
        sb.append(part1);
        return sb.toString();
    }

    public Map<String, List<DeepSearchHitListDTO<?>>> parseResponse(Response response) {
        Map<String, List<DeepSearchHitListDTO<?>>> ret = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(response.getEntity().getContent());
            JsonNode buckets = root.get("aggregations").get("group_by_terminology").get("buckets");
            for (JsonNode bucket : buckets) {
                JsonNode meta = bucket.get("top_terminology_hits").get("hits");
                int total = meta.get("total").intValue();
                if (total > 0) {
                    String terminologyId = bucket.get("key").textValue();
                    List<ConceptSimpleDTO> topHits = new ArrayList<>();
                    DeepSearchConceptHitListDTO hitList = new DeepSearchConceptHitListDTO(total, topHits);
                    ret.put(terminologyId, Collections.singletonList(hitList));

                    JsonNode hits = meta.get("hits");
                    for (JsonNode hit : hits) {
                        JsonNode concept = hit.get("_source");
                        String conceptId = concept.get("id").textValue();
                        String conceptUri = textValue(concept.get("uri"));
                        String conceptStatus = textValue(concept.get("status"));
                        Map<String, String> labelMap = ElasticRequestUtils.labelFromKeyValueNode(concept.get("label"));

                        ConceptSimpleDTO dto = new ConceptSimpleDTO(conceptId, conceptUri, conceptStatus, labelMap);
                        topHits.add(dto);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot parse deep concept query response", e);
        }
        return ret;
    }

    private String textValue(JsonNode node) {
        if (node == null) {
            return null;
        }
        return node.textValue();
    }
}
