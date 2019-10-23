package fi.vm.yti.terminology.api.frontend.elasticqueries;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.terminology.api.frontend.searchdto.ConceptDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.ConceptSearchRequest;
import fi.vm.yti.terminology.api.frontend.searchdto.ConceptSearchResponse;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySimpleDTO;
import fi.vm.yti.terminology.api.util.ElasticRequestUtils;

public class ConceptQueryFactory {
    private final static boolean CONFIG_ONLY_CHECK_TERMINOLOGY_STATE = true;
    private final static boolean CONFIG_DO_NOT_CHECK_TERMINOLOGY_STATE_FOR_GIVEN_TERMINOLOGIES = true;
    private final static boolean CONFIG_DO_NOT_CHECK_TERMINOLOGY_STATE_FOR_GIVEN_CONCEPTS = true;
    private final static boolean CONFIG_DO_NOT_CHECK_CONCEPT_STATE_FOR_GIVEN_CONCEPTS = true;

    private static final Logger log = LoggerFactory.getLogger(ConceptQueryFactory.class);

    private final ObjectMapper objectMapper;
    private final Pattern terminologyCodePattern;

    public ConceptQueryFactory(ObjectMapper objectMapper,
                               String namespaceRoot) {
        this.objectMapper = objectMapper;
        this.terminologyCodePattern = Pattern.compile(Pattern.quote(namespaceRoot + "[^/]+/([^/]+)"));
    }

    public SearchRequest createQuery(ConceptSearchRequest request,
                                     boolean superUser,
                                     MatchingTerminologyResolver matchingTerminologyResolver) {

        List<QueryBuilder> mustParts = new ArrayList<>();

        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            MultiMatchQueryBuilder labelQuery = QueryBuilders.multiMatchQuery(request.getQuery(), "label.*")
                .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX)
                .minimumShouldMatch("90%");
            if (request.getSortLanguage() != null && ElasticRequestUtils.LANGUAGE_CODE_PATTERN.matcher(request.getSortLanguage()).matches()) {
                labelQuery = labelQuery.field("label." + request.getSortLanguage(), 10);
            }
            mustParts.add(labelQuery);
        }

        final boolean directConceptsGiven =  request.getConceptId() != null && request.getConceptId().length > 0;
        if (directConceptsGiven) {
            QueryBuilder conceptIdQuery = QueryBuilders.termsQuery("id", request.getConceptId());
            mustParts.add(conceptIdQuery);
        }

        final boolean directTerminologiesGiven = request.getTerminologyId() != null && request.getTerminologyId().length > 0;
        if (directTerminologiesGiven) {
            QueryBuilder terminologyIdQuery = QueryBuilders.termsQuery("vocabulary.id", request.getTerminologyId());
            mustParts.add(terminologyIdQuery);
        }

        if (request.getNotInTerminologyId() != null && request.getNotInTerminologyId().length > 0) {
            QueryBuilder notInTerminologyIdQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.termsQuery("vocabulary.id", request.getNotInTerminologyId()));
            mustParts.add(notInTerminologyIdQuery);
        }

        final boolean directBroaderGiven = request.getBroaderConceptId() != null && request.getBroaderConceptId().length > 0;
        if (directBroaderGiven) {
            QueryBuilder broaderConceptIdQuery = QueryBuilders.termsQuery("broader", request.getBroaderConceptId());
            mustParts.add(broaderConceptIdQuery);
        }

        if (request.getOnlyTopConcepts() != null && request.getOnlyTopConcepts()) {
            QueryBuilder onlyTopConceptsQuery = QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery("broader"));
            mustParts.add(onlyTopConceptsQuery);
        }

        if (request.getStatus() != null && request.getStatus().length > 0) {
            QueryBuilder statusQuery = QueryBuilders.termsQuery("status", request.getStatus());
            mustParts.add(statusQuery);
        }

        // Checks regarding data in INCOMPLETE state. Basic idea is to show INCOMPLETE things only to the contributors. But.
        if (!superUser) {
            final boolean doNotCheckTerminologyState = (directTerminologiesGiven && CONFIG_DO_NOT_CHECK_TERMINOLOGY_STATE_FOR_GIVEN_TERMINOLOGIES) ||
                ((directConceptsGiven || directBroaderGiven) && CONFIG_DO_NOT_CHECK_TERMINOLOGY_STATE_FOR_GIVEN_CONCEPTS);
            final boolean checkTerminologyState = !doNotCheckTerminologyState;
            final boolean doNotCheckConceptState = CONFIG_ONLY_CHECK_TERMINOLOGY_STATE ||
                ((directConceptsGiven || directBroaderGiven) && CONFIG_DO_NOT_CHECK_CONCEPT_STATE_FOR_GIVEN_CONCEPTS);
            final boolean checkConceptState = !doNotCheckConceptState;
            if (checkConceptState || checkTerminologyState) {
                Collection<String> incompleteFromTerminologies = matchingTerminologyResolver.resolveMatchingTerminologies(directTerminologiesGiven ? Arrays.asList(request.getTerminologyId()) : null);
                QueryBuilder contributorQuery = QueryBuilders.termsQuery("vocabulary.id", incompleteFromTerminologies);
                BoolQueryBuilder statusQuery = QueryBuilders.boolQuery();
                if (checkConceptState) {
                    statusQuery.mustNot(QueryBuilders.termQuery("status", "INCOMPLETE"));
                }
                if (checkTerminologyState) {
                    statusQuery.mustNot(QueryBuilders.termQuery("vocabulary.status", "INCOMPLETE"));
                }
                mustParts.add(QueryBuilders.boolQuery().should(contributorQuery).should(statusQuery).minimumShouldMatch(1));
            }
        }

        QueryBuilder combinedQuery = null;
        if (mustParts.isEmpty()) {
            combinedQuery = QueryBuilders.matchAllQuery();
        } else if (mustParts.size() == 1) {
            combinedQuery = mustParts.get(0);
        } else {
            final BoolQueryBuilder tmp = QueryBuilders.boolQuery();
            mustParts.forEach(tmp::must);
            combinedQuery = tmp;
        }

        SearchSourceBuilder ssb = new SearchSourceBuilder()
            .query(combinedQuery)
            .size(request.getPageSize() != null ? request.getPageSize().intValue() : 100)
            .from(request.getPageFrom() != null ? request.getPageFrom().intValue() : 0);

        ConceptSearchRequest.SortBy sortBy = request.getSortBy() != null ? request.getSortBy() : ConceptSearchRequest.SortBy.PREF_LABEL;
        ConceptSearchRequest.SortDirection sortDirection = request.getSortDirection() != null ? request.getSortDirection() : ConceptSearchRequest.SortDirection.ASC;
        if (sortBy == ConceptSearchRequest.SortBy.MODIFIED) {
            ssb.sort(SortBuilders.fieldSort("modified").order(sortDirection.getEsOrder()));
        }
        String sortLanguage = request.getSortLanguage() != null && !request.getSortLanguage().isEmpty() ? request.getSortLanguage() : "fi";
        ssb.sort(SortBuilders.fieldSort("sortByLabel." + sortLanguage).order(sortBy == ConceptSearchRequest.SortBy.PREF_LABEL ? sortDirection.getEsOrder() : SortOrder.ASC));

        SearchRequest sr = new SearchRequest("concepts").source(ssb);
        log.debug("Concept Query request: " + sr.toString());
        return sr;
    }

    public ConceptSearchResponse parseResponse(SearchResponse response,
                                               int pageFrom) {
        if (response != null) {
            SearchHits hitsContainer = response.getHits();
            if (hitsContainer != null) {
                final long total = hitsContainer.getTotalHits();
                final SearchHit[] hits = hitsContainer.getHits();
                final List<ConceptDTO> concepts = new ArrayList<>();
                if (hits != null) {
                    for (SearchHit hit : hits) {
                        try {
                            final JsonNode concept = objectMapper.readTree(hit.getSourceAsString());
                            final String id = ElasticRequestUtils.getTextValueOrNull(concept, "id");
                            final String uri = ElasticRequestUtils.getTextValueOrNull(concept, "uri");
                            final String status = ElasticRequestUtils.getTextValueOrNull(concept, "status");
                            final Map<String, String> labelMap = ElasticRequestUtils.labelFromKeyValueNode(concept.get("label"));
                            final Map<String, String> altLabelMap = ElasticRequestUtils.labelFromKeyValueNode(concept.get("altLabel"));
                            final Map<String, String> definitionMap = ElasticRequestUtils.labelFromKeyValueNode(concept.get("definition"));
                            final String modifiedString = ElasticRequestUtils.getTextValueOrNull(concept, "modified");
                            Instant modified = null;
                            if (modifiedString != null) {
                                try {
                                    modified = ZonedDateTime.parse(modifiedString).toInstant();
                                } catch (Exception e) {
                                    log.warn("Could not parse modified timestamp", e);
                                }
                            }
                            final List<String> narrower = getIdList(concept, "narrower");
                            final List<String> broader = getIdList(concept, "broader");

                            TerminologySimpleDTO terminology = null;
                            final JsonNode terminologyNode = concept.get("vocabulary");
                            if (terminologyNode != null) {
                                final String terminologyId = ElasticRequestUtils.getTextValueOrNull(terminologyNode, "id");
                                final String terminologyStatus = ElasticRequestUtils.getTextValueOrNull(terminologyNode, "status");
                                final String terminologyUri = ElasticRequestUtils.getTextValueOrNull(terminologyNode, "uri");
                                final Map<String, String> terminologyLabelMap = ElasticRequestUtils.labelFromKeyValueNode(terminologyNode.get("label"));
                                String terminologyCode = null;
                                if (terminologyUri != null) {
                                    Matcher m = terminologyCodePattern.matcher(terminologyUri);
                                    if (m.matches()) {
                                        terminologyCode = m.group(1);
                                    }
                                }
                                terminology = new TerminologySimpleDTO(terminologyId, terminologyCode, terminologyUri, terminologyStatus, terminologyLabelMap);
                            }

                            concepts.add(new ConceptDTO(id, uri, status, labelMap, altLabelMap, definitionMap, modified, narrower, broader, terminology));
                        } catch (Exception e) {
                            log.error("Error while parsing a concept hit", e);
                        }
                    }
                }
                return new ConceptSearchResponse(total, pageFrom, concepts);
            }
        }
        return new ConceptSearchResponse();
    }

    private List<String> getIdList(JsonNode node,
                                   String field) {
        List<String> ret = null;
        JsonNode arrayNode = node.get(field);
        if (arrayNode != null && arrayNode.isArray() && arrayNode.size() > 0) {
            ret = new ArrayList<>();
            for (JsonNode idNode : arrayNode) {
                String id = idNode.textValue();
                if (id != null && !id.isEmpty()) {
                    ret.add(id);
                }
            }
            ret.sort(String::compareTo);
        }
        if (ret != null && ret.isEmpty()) {
            return null;
        }
        return ret;
    }

    @FunctionalInterface
    public interface MatchingTerminologyResolver {

        Set<String> resolveMatchingTerminologies(Collection<String> limitToTheseTerminologyIds);
    }
}
