package fi.vm.yti.terminology.api.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.frontend.FrontendGroupManagementService;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.index.IndexElasticSearchService;
import fi.vm.yti.terminology.api.integration.containers.ContainersResponse;
import fi.vm.yti.terminology.api.integration.containers.Description;
import fi.vm.yti.terminology.api.integration.containers.PrefLabel;
import fi.vm.yti.terminology.api.model.integration.ConceptSuggestion;
import fi.vm.yti.terminology.api.model.integration.IntegrationResourceRequest;
import fi.vm.yti.terminology.api.model.integration.Meta;
import fi.vm.yti.terminology.api.model.integration.ResponseWrapper;
import fi.vm.yti.terminology.api.model.integration.IntegrationContainerRequest;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import fi.vm.yti.terminology.api.util.JsonUtils;
import fi.vm.yti.terminology.api.model.termed.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@Service
public class IntegrationService {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);
    private static final Set<String> sortLanguages = new HashSet<>(Arrays.asList("fi", "en", "sv"));
    private final TermedRequester termedRequester;
    private final FrontendGroupManagementService groupManagementService;
    private final FrontendTermedService termedService;
    private final IndexElasticSearchService elasticSearchService;
    private final AuthenticatedUserProvider userProvider;
    private final String indexName;
    private final String VOCABULARY_INDEX = "vocabularies";
    private final String CONCEPTS_INDEX = "concepts";

    /**
     * Map containing metadata types. used when creating nodes.
     */
    private HashMap<String, MetaNode> typeMap = new HashMap<>();

    @Autowired
    public IntegrationService(TermedRequester termedRequester, FrontendGroupManagementService groupManagementService,
            FrontendTermedService frontendTermedService, IndexElasticSearchService elasticSearchService,
            AuthenticatedUserProvider userProvider, @Value("${search.index.name}") String indexName) {
        this.termedRequester = termedRequester;
        this.groupManagementService = groupManagementService;
        this.termedService = frontendTermedService;
        this.elasticSearchService = elasticSearchService;
        this.userProvider = userProvider;
        this.indexName = indexName;
    }

    ResponseEntity<String> handleContainers(IntegrationContainerRequest request) {

        if (logger.isDebugEnabled())
            logger.debug("GET /containers requested. status=" + request.getStatus());

        SearchRequest sr = createVocabularyQuery(request);
        if (logger.isDebugEnabled()) {
            logger.debug("HandleVocabularies() query=" + sr.source().toString());
        }

        JsonNode r = elasticSearchService.freeSearchFromIndex(sr);

        // logger.info(" result:" + JsonUtils.prettyPrintJsonAsString(r));
        // Use highLevel API result so
        logger.debug("Raw json-result:" + r);
        Meta meta = new Meta();
        meta.setAfter(request.getAfter());
        meta.setPageSize(request.getPageSize());
        meta.setFrom(request.getPageFrom());
        // Response item list
        List<ContainersResponse> resp = new ArrayList<>();
        if (r != null) {
            r = r.get("hits");
            // Total hits
            if (r.get("total") != null) {
                meta.setTotalResults(r.get("total").asInt());
            }
            r = r.get("hits");
            r.forEach(hit -> {
                JsonNode source = hit.get("_source");
                if (source != null) {
                    // System.out.println("containers-Response=" +
                    // JsonUtils.prettyPrintJsonAsString(source));
                    resp.add(parseContainerResponse(source));
                } else {
                    logger.error("r-hit=" + hit);
                }
            });
            meta.setResultCount(r.size());
        }

        ResponseWrapper<ContainersResponse> wrapper = new ResponseWrapper<>();
        wrapper.setMeta(meta);
        wrapper.setResults(resp);
        /*
         * prints data without newlines. ObjectMapper mapper = new ObjectMapper(); try {
         * System.out.println(" mapper.write=" + mapper.writeValueAsString(wrapper)); }
         * catch (JsonProcessingException jpe) { }
         */
        return new ResponseEntity<>(JsonUtils.prettyPrintJsonAsString(wrapper),
                HttpStatus.OK);/**
                                * Elastic query, returns 10k results from index and filter out items without
                                * URI
                                */
        // String query = "{ \"query\" : {\"bool\":{\"must\": {\"match_all\" :
        // {}},\"filter\": {\"exists\": { \"field\": \"uri\"}
        // }}},\"size\":\"10000\",\"_source\":[\"id\",\"properties.prefLabel\",\"properties.description\",\"lastModifiedDate\",\"properties.status\",\"uri\"]}";
    }

    private SearchRequest createVocabularyQuery(IntegrationContainerRequest request) {
        // String query, Set<String> status, Date after, Integer pageSize, Integer
        // pageFrom, QueryBuilder privilegeQuery,Set<String> filter
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        List<QueryBuilder> mustList = boolQuery.must();

        // mustList.add(QueryBuilders.matchAllQuery());

        if (request.getAfter() != null) {
            mustList.add(QueryBuilders.rangeQuery("modified").gte(request.getAfter()));
        }
        // Add mandatory filter: "filter": {"exists": { "field": "uri"} }
        /*
         * QueryBuilder urlExistQuery = QueryBuilders.boolQuery()
         * .must(QueryBuilders.existsQuery("url")); mustList.add(urlExistQuery);
         */
        if (request.getFilter() != null) {
            String u = request.getFilter().toString();
            request.getFilter().forEach(o -> {
                QueryBuilder fq = QueryBuilders.boolQuery().mustNot(QueryBuilders.wildcardQuery("uri", o + "*"));
                mustList.add(fq);
            });
            /*
             * u="http://uri.suomi.fi/terminology/rak/"; QueryBuilder filterQuery =
             * QueryBuilders.boolQuery() .mustNot(QueryBuilders.termsQuery("uri", u));
             * mustList.add(filterQuery);
             */
        }

        if (request.getLanguage() != null && !request.getLanguage().isEmpty()) {
            QueryBuilder langQuery = null;
            Set<String> lq = request.getLanguage();
            logger.info("Lang query set");
            // add actual status filtering
            langQuery = QueryBuilders.boolQuery().should(QueryBuilders.termsQuery("properties.language.value", lq))
                    .minimumShouldMatch(1);
            mustList.add(langQuery);
        }

        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            Set<String> sq = request.getStatus();
            logger.info("Status query set");

            if (request.getStatus().contains("INCOMPLETE") && !request.getIncludeIncomplete()) {
                // remove incomplete if not specifially asked
                sq.remove("INCOMPLETE");
            }

            BoolQueryBuilder statusBoolQuery = QueryBuilders.boolQuery();
            // add actual status filtering
            sq.forEach(o -> {
                statusBoolQuery.should(QueryBuilders.matchQuery("properties.status.value", o));
            });

            statusBoolQuery.minimumShouldMatch(1);
            mustList.add(statusBoolQuery);
        } else {
            QueryBuilder statusQuery = null;
            logger.info("Status empty, so use default and filter out incomplete. if flag is not set");
            if (!request.getIncludeIncomplete()) {
                statusQuery = QueryBuilders.boolQuery()
                        .mustNot(QueryBuilders.matchQuery("properties.status.value", "INCOMPLETE"))
                        .minimumShouldMatch(1);
                mustList.add(statusQuery);
            }
        }

        // Don't return items without URI
        mustList.add(QueryBuilders.existsQuery("uri"));

        if (mustList.size() > 0) {

            logger.info("Multiple matches:" + mustList.size());
            mustList.forEach(o -> {
                System.out.println(o.toString());
            });

            sourceBuilder.query(boolQuery);
        } else {
            logger.info("ALL matches");
            sourceBuilder.query(QueryBuilders.matchAllQuery());
        }

        if (request.getPageFrom() != null) {
            sourceBuilder.from(request.getPageFrom());
        }

        if (request.getPageSize() != null && request.getPageSize() > 0) {
            sourceBuilder.size(request.getPageSize());
            if (request.getPageFrom() == null) {
                sourceBuilder.from(0);
            }
        } else {
            sourceBuilder.size(10000);
        }

        String[] includeFields = new String[] { "id", "properties.prefLabel", "properties.language",
                "properties.description", "lastModifiedDate", "properties.status.value", "uri",
                "references.contributor.id" };
        sourceBuilder.fetchSource(includeFields, null);
        // Add endpoint into the request
        SearchRequest sr = new SearchRequest(VOCABULARY_INDEX).source(sourceBuilder);
        // Add label sorting according to label
        /*
         * if (request.getLanguage() != null) { request.getLanguage().forEach(o-> {
         * addLanguagePrefLabelSort(o, "url", "url", sourceBuilder); } ); }
         */
        if (logger.isDebugEnabled()) {
            logger.debug("SearchRequest=" + sr);
            logger.debug(sr.source().toString());
        }
        return sr;
    }

    /**
     * Transform containers response from elastic to integration-api JSON format
     * 
     * @param source
     * @return
     */
    private ContainersResponse parseContainerResponse(JsonNode source) {
        ContainersResponse respItem = new ContainersResponse();
        if(logger.isDebugEnabled()){
            logger.debug("Parse incoming:\n" + JsonUtils.prettyPrintJsonAsString(source));
        }
        // Some vocabularies has no status at all
        String stat = null;
        if (source.findPath("status") != null) {
            stat = source.findPath("status").findPath("value").asText();
        } else {
            // default is DRAFT
            stat = "DRAFT";
        }
        if (stat != null && !stat.isEmpty()) {
            respItem.setStatus(stat);
        }

        String modifiedDate = null;
        if (source.get("lastModifiedDate") != null) {
            modifiedDate = source.get("lastModifiedDate").asText();
        }
        // http://uri.suomi.fi/terminology/2/terminological-vocabulary-0
        // Get uri and remove last part after /
        // TODO! after 1 graph change, reve this part
        String uri = null;
        if (source.get("uri") != null) {
            uri = source.get("uri").asText();
            // Remove code from uri so
            uri = uri.substring(0, uri.lastIndexOf("/")) + "/";
        }
        respItem.setUri(uri);

        if (modifiedDate != null) {
            // Curently returns 2019-01-07T09:16:32.432+02:00
            // use only first 19 chars
            respItem.setModified(modifiedDate.substring(0, 19));
        }
        JsonNode label = source.findPath("prefLabel");
        if (label != null) {
            PrefLabel plab = new PrefLabel();
            label.forEach(lb -> {
                String lan = null;
                String val = null;
                if (lb.findPath("lang") != null) {
                    lan = lb.findPath("lang").asText();
                }
                if (lb.findPath("value") != null) {
                    val = lb.findPath("value").asText();
                }
                if (lan != null) {
                    if (lan.equalsIgnoreCase("fi")) {
                        plab.setFi(val);
                    } else if (lan.equalsIgnoreCase("en")) {
                        plab.setEn(val);
                    } else if (lan.equalsIgnoreCase("sv")) {
                        plab.setSv(val);
                    }
                }
            });
            respItem.setPrefLabel(plab);
        }

        JsonNode description = source.findPath("description");
        if (description != null) {
            Description desc = new Description();
            description.forEach(de -> {
                String lan = null;
                String val = null;
                if (de.findPath("lang") != null) {
                    lan = de.findPath("lang").asText();
                }
                if (de.findPath("value") != null) {
                    val = de.findPath("value").asText();
                    val = Jsoup.clean(val, Whitelist.none());
                }
                if (lan != null) {
                    if (lan.equalsIgnoreCase("fi")) {
                        desc.setFi(val);
                    } else if (lan.equalsIgnoreCase("en")) {
                        desc.setEn(val);
                    } else if (lan.equalsIgnoreCase("sv")) {
                        desc.setSv(val);
                    }
                }
            });
            respItem.setDescription(desc);

            List<String> languageList = new ArrayList<String>();
            // Add hard-coded language-list
            JsonNode lang = source.findPath("language");
            if (lang != null) {
                Description d = new Description();
                lang.forEach(de -> {
                    String val = null;
                    if (de.findPath("value") != null) {
                        val = de.findPath("value").asText();
                        val = Jsoup.clean(val, Whitelist.none());
                        languageList.add(val);
                    }
                });
                if (logger.isDebugEnabled()) {
                    logger.debug("Vocabulary LANGS=" + languageList);
                }
                respItem.setLanguage(languageList);
            }
        }
        return respItem;
    }

    ResponseEntity<String> handleResources(IntegrationResourceRequest request) {
        if (logger.isDebugEnabled())
            logger.debug("(GET/POST) /resources requested. URL=" + request.getContainer() + " UriSet="
                    + request.getFilter());

        UUID id = null;
        List<Graph> vocs = termedService.getGraphs();
        for (Graph g : vocs) {
            if (g.getUri() != null && !g.getUri().isEmpty() && g.getUri().equals(request.getContainer())) {
                id = g.getId();
            }
        }
        // Uri given and id not found, genuine 404 Not Found error
        if (id == null && request.getContainer() != null && !request.getContainer().isEmpty()) {
            return new ResponseEntity<>("{}", HttpStatus.NOT_FOUND);
        }

        // Id resolved, fetch vocabulary and filter out vocabularies without UR
        SearchRequest sr = createResourcesQuery(request, id);
        if (logger.isDebugEnabled()) {
            logger.debug("HandleVocabularies() query=" + sr.source().toString());
        }

        JsonNode r = elasticSearchService.freeSearchFromIndex(sr);
        // Use highLevel API result so
        logger.debug("raw json-result:" + r);

        Meta meta = new Meta();
        meta.setAfter(request.getAfter());
        meta.setPageSize(request.getPageSize());
        meta.setFrom(request.getPageFrom());
        // Response item list
        List<ContainersResponse> resp = new ArrayList<>();
        if (r != null) {
            r = r.get("hits");
            // Total hits
            if (r.get("total") != null) {
                meta.setTotalResults(r.get("total").asInt());
            }
            r = r.get("hits");
            r.forEach(hit -> {
                JsonNode source = hit.get("_source");
                if (source != null) {
//                    System.out.println("resources-Response=" + JsonUtils.prettyPrintJsonAsString(source));

                    ContainersResponse node = parseResourceResponse(source);
                    if (node.getUri() != null && node.getPrefLabel() != null && node.getStatus() != null) {
                        resp.add(node);
                    } else {
                        logger.error("Resource response missing mandatory fields. dropping " + node);
                    }
                } else {
                    logger.error("handleResources hit=" + hit);
                }
            });
            meta.setResultCount(r.size());
        } else {
            // Empty result list
            meta.setResultCount(0);
        }
        System.out.println("total META=" + meta.getTotalResults());
        System.out.println("current block  META=" + meta.getResultCount());

        /**
         * Elastic query, returns 10k results from index
         * 
         * { "query" : { "bool":{ "must": { "match": {
         * "vocabulary.id":"cd8fed1b-7f1c-4e2d-b307-a7662286f713" } }, "filter": {
         * "exists": { "field": "uri"} } } }, "size":"10000",
         * "_source":["id","label","definition","modified", "status","uri"] }
         * 
         * GET /_search
         */

        ResponseWrapper<ContainersResponse> wrapper = new ResponseWrapper<>();
        wrapper.setMeta(meta);
        wrapper.setResults(resp);
        /*
         * prints data without newlines. ObjectMapper mapper = new ObjectMapper(); try {
         * System.out.println(" marrer.write=" + mapper.writeValueAsString(wrapper)); }
         * catch (JsonProcessingException jpe) { }
         */
        return new ResponseEntity<>(JsonUtils.prettyPrintJsonAsString(wrapper), HttpStatus.OK);
    }

    /**
     * { "searchTerm":"string", "language":"string", "container":"string", "status":
     * [ "string" ], "after":"2019-09-11T09:27:29.964Z", "filter":[ "string" ],
     * "pageSize":0, "pageFrom":0 }
     * 
     * @param request
     * @param vocabularyId
     * @return
     */
    private SearchRequest createResourcesQuery(IntegrationResourceRequest request, UUID vocabularyId) {
        /*
         * { "query" : { "bool":{ "must": { "match": {
         * "vocabulary.id":"cd8fed1b-7f1c-4e2d-b307-a7662286f713" } }, "filter": {
         * "exists": { "field": "uri"} } } }, "size":"10000",
         * "_source":["id","properties.prefLabel","properties.description",
         * "lastModifiedDate","properties.status","uri"]
         */

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        LuceneQueryFactory luceneQueryFactory = new LuceneQueryFactory();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        List<QueryBuilder> mustList = boolQuery.must();
        // if searh-term is given, match for all labels
        if (request.getSearchTerm() != null) {
            logger.info("Additional SearchTerm=" + request.getSearchTerm());
            QueryStringQueryBuilder labelQuery = luceneQueryFactory.buildPrefixSuffixQuery(request.getSearchTerm())
                    .field("label.*");
            mustList.add(labelQuery);
            final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder
                    .should(luceneQueryFactory.buildPrefixSuffixQuery(request.getSearchTerm()).field("label.*"));
            boolQueryBuilder.minimumShouldMatch(1);
            // mustList.add(boolQueryBuilder);
            logger.info("Additional lucene query=" + boolQueryBuilder.toString());
        }
        /**
         * if (searchTerm != null && !searchTerm.isEmpty()) { final BoolQueryBuilder
         * boolQueryBuilder = boolQuery();
         * boolQueryBuilder.should(luceneQueryFactory.buildPrefixSuffixQuery(searchTerm).field("prefLabel.*"));
         * boolQueryBuilder.should(luceneQueryFactory.buildPrefixSuffixQuery(searchTerm).field("codeValue"));
         * if (!codeSchemeUuids.isEmpty()) { boolQueryBuilder.should(termsQuery("id",
         * codeSchemeUuids)); } boolQueryBuilder.minimumShouldMatch(1);
         * builder.must(boolQueryBuilder); }
         */

        // Match vocabularyId or if missing, use other matches
        if (vocabularyId != null) {
            mustList.add(QueryBuilders.matchQuery("vocabulary.id", vocabularyId.toString()));
        }

        if (request.getAfter() != null) {
            mustList.add(QueryBuilders.rangeQuery("modified").gte(request.getAfter()));
        }

        if (request.getFilter() != null) {
            logger.info("Exclude filter:" + request.getFilter());
            QueryBuilder filterQuery = QueryBuilders.boolQuery()
                    .mustNot(QueryBuilders.termsQuery("uri", request.getFilter()));
            mustList.add(filterQuery);
        }

        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            QueryBuilder statusQuery = QueryBuilders.boolQuery()
                    .should(QueryBuilders.termsQuery("status", request.getStatus())).minimumShouldMatch(1);
            mustList.add(statusQuery);
        }

        // if search-term is given, match for all labels containing it
        if (request.getSearchTerm() != null) {
            QueryStringQueryBuilder labelQuery = luceneQueryFactory.buildPrefixSuffixQuery(request.getSearchTerm())
                    .field("label.*");
            mustList.add(labelQuery);
        }

        // Don't return items without URI
        mustList.add(QueryBuilders.existsQuery("uri"));

        sourceBuilder.query(boolQuery);

        if (request.getPageFrom() != null) {
            sourceBuilder.from(request.getPageFrom());
        }

        if (request.getPageSize() != null && request.getPageSize() > 0) {
            sourceBuilder.size(request.getPageSize());
            if (request.getPageFrom() == null) {
                sourceBuilder.from(0);
            }
        } else {
            sourceBuilder.size(10000);
        }
        String[] includeFields = new String[] { "id", "label", "definition", "modified", "status", "uri" };
        sourceBuilder.fetchSource(includeFields, null);
        // Add endpoint into the request
        SearchRequest sr = new SearchRequest(CONCEPTS_INDEX).source(sourceBuilder);
        // Add label sorting according to label
        if (request.getLanguage() != null && !request.getLanguage().isEmpty()) {
            addLanguagePrefLabelSort(request.getLanguage(), "sortByLabel.fi", "label", sourceBuilder);
        }
        // if (logger.isDebugEnabled()) {
        logger.info("SearchRequest=" + sr);
        logger.debug(sr.source().toString());
        // }
        return sr;
    }

    /** Transform incoming response into the resource-api JSON form */
    private ContainersResponse parseResourceResponse(JsonNode source) {
        String stat = null;
        String modifiedDate = null;
        String uri = null;
        if (source.get("status") != null) {
            stat = source.get("status").asText();
        } else {
            logger.warn("Resource response missing status");
        }
        if (source.get("modified") != null) {
            modifiedDate = source.get("modified").asText();
        } else {
            logger.warn("Resource response missing modified date");
        }
        if (source.get("uri") != null) {
            uri = source.get("uri").asText();
        } else {
            logger.warn("Resource response missing URI");
        }

        ContainersResponse respItem = new ContainersResponse();
        respItem.setUri(uri);
        respItem.setStatus(stat);
        if (modifiedDate != null) {
            // Curently returns 2019-01-07T09:16:32.432+02:00
            // use only first 19 chars
            respItem.setModified(modifiedDate.substring(0, 19));
        }
        JsonNode label = source.get("label");
        if (label != null) {
            PrefLabel plab = new PrefLabel();
            // fi
            JsonNode lan = label.get("fi");
            if (lan != null) {
                plab.setFi(Jsoup.clean(lan.get(0).asText(), Whitelist.none()));
            }
            // en
            lan = label.get("en");
            if (lan != null) {
                plab.setEn(Jsoup.clean(lan.get(0).asText(), Whitelist.none()));
            }
            // sv
            lan = label.get("sv");
            if (lan != null) {
                plab.setSv(Jsoup.clean(lan.get(0).asText(), Whitelist.none()));
            }
            respItem.setPrefLabel(plab);
        }

        JsonNode description = source.get("definition");
        if (description != null) {
            Description desc = new Description();
            // fi
            JsonNode d = description.get("fi");
            if (d != null) {
                desc.setFi(Jsoup.clean(d.get(0).asText(), Whitelist.none()));
            }
            // en
            d = label.get("en");
            if (d != null) {
                desc.setEn(Jsoup.clean(d.get(0).asText(), Whitelist.none()));
            }
            // sv
            d = label.get("sv");
            if (d != null) {
                desc.setSv(Jsoup.clean(d.get(0).asText(), Whitelist.none()));
            }
            respItem.setDescription(desc);
        }
        return respItem;
    }

    private void addLanguagePrefLabelSort(final String language, final String backupSortField,
            final String sortFieldWithoutLanguage, final SearchSourceBuilder searchBuilder) {
        if (language != null && !language.isEmpty()) {
            searchBuilder.sort(SortBuilders.fieldSort("label." + language + ".keyword").order(SortOrder.ASC)
                    .unmappedType("keyword"));
            sortLanguages.forEach(sortLanguage -> {
                if (!language.equalsIgnoreCase(sortLanguage)) {
                    searchBuilder.sort(SortBuilders.fieldSort("label." + sortLanguage + ".keyword").order(SortOrder.ASC)
                            .unmappedType("keyword"));
                }
            });
            searchBuilder.sort(backupSortField, SortOrder.ASC);
        } else {
            searchBuilder.sort(sortFieldWithoutLanguage, SortOrder.ASC);
        }
    }

    /**
     * Initialize cached META-model. - Read given vocabularity for meta-types, cache
     * them
     *
     * @param vocabularityId UUID of the vocabularity
     */
    private void initMetaModel(UUID vocabularityId) {
        // Get metamodel types for given vocabularity
        List<MetaNode> metaTypes = termedService.getTypes(vocabularityId);
        metaTypes.forEach(t -> {
            typeMap.put(t.getId(), t);
        });
    }

    /**
     * Executes concept suggestion operation. Reads incoming json and process it
     *
     * @param vocabularityId
     * @return
     */
    ResponseEntity<String> handleConceptSuggestion(String terminologyUri, ConceptSuggestion incomingConcept) {
        if (logger.isDebugEnabled())
            logger.debug("POST /vocabulary/{vocabularyId}/concept requested. creating Concept for "
                    + JsonUtils.prettyPrintJsonAsString(incomingConcept));
        UUID activeVocabulary;
        // Concept reference-map
        Map<String, List<Identifier>> conceptReferences = new HashMap<>();

        // Get vocabularies and match code with name
        List<Graph> vocs = termedService.getGraphs();
        // Filter given code as result
        List<IdCode> vocabularies = vocs.stream().filter(o -> o.getUri().equalsIgnoreCase(terminologyUri)).map(o -> {
            return new IdCode(o.getCode(), o.getId());
        }).collect(Collectors.toList());
        if (vocabularies.size() > 1) {
            return new ResponseEntity<>("Created Concept suggestion failed for " + terminologyUri
                    + ". Multiple matches for terminology. \n", HttpStatus.NOT_FOUND);
        } else if (vocabularies.size() == 1) {
            // found, set UUID
            activeVocabulary = vocabularies.get(0).id;
        } else {
            // It may be UUID, so try to convert that
            // Try if it is UUID
            try {
                activeVocabulary = UUID.fromString(terminologyUri);
            } catch (IllegalArgumentException ex) {
                // Not UUID, error.
                return new ResponseEntity<>(
                        "Created Concept suggestion failed for " + terminologyUri + ". Terminology not found. \n",
                        HttpStatus.NOT_FOUND);
            }
        }

        // Try to fetch it just to ensure it exist
        GenericNodeInlined vocabularyNode = termedService.getVocabulary(activeVocabulary);
        if (vocabularyNode == null) {
            return new ResponseEntity<>(
                    "Created Concept suggestion failed for UUID:" + terminologyUri + ". Terminology not found. \n",
                    HttpStatus.NOT_FOUND);
        }

        // get metamodel for vocabulary
        initMetaModel(activeVocabulary);
        // Create new Term
        GenericNode term = CreateTerm(vocabularyNode, incomingConcept, conceptReferences);
        // Create new Concept
        GenericNode concept = CreateConcept(vocabularyNode, incomingConcept, conceptReferences);
        if (term != null && concept != null) {
            incomingConcept.setVocabulary(vocabularyNode.getId());
            if (userProvider.getUser() != null && userProvider.getUser().getId() != null) {
                incomingConcept.setCreator(userProvider.getUser().getId().toString());
            }
            // Publish them to server
            List<GenericNode> addNodeList = new ArrayList<>();
            addNodeList.add(term);
            addNodeList.add(concept);
            GenericDeleteAndSave operation = new GenericDeleteAndSave(emptyList(), addNodeList);
            termedService.bulkChangeWithoutAuthorization(operation, true,
                    UUID.fromString(incomingConcept.getCreator()));
            if (logger.isDebugEnabled())
                logger.debug(JsonUtils.prettyPrintJsonAsString(operation));
            // Fetch created concept and get it's URI, set it to the returned json
            // Return also it's UUID
            GenericNode createdConcept = termedService.getConceptNode(activeVocabulary, concept.getId());
            incomingConcept.setUri(createdConcept.getUri());
            incomingConcept.setIdentifier(createdConcept.getId());
        }
        return new ResponseEntity<>(JsonUtils.prettyPrintJsonAsString(incomingConcept), HttpStatus.OK);
    }

    private GenericNode CreateTerm(GenericNodeInlined vocabulary, ConceptSuggestion incoming,
            Map<String, List<Identifier>> parentReferences) {
        GenericNode node = null;
        // Populate term
        Map<String, List<Attribute>> properties = new HashMap<>();
        addProperty("prefLabel", properties, incoming.getPrefLabel());
        Attribute att = new Attribute("", "SUGGESTED");
        addProperty("status", properties, att);
        // Create Concept
        TypeId typeId = typeMap.get("Term").getDomain();
        node = new GenericNode(typeId, properties, emptyMap());

        // Add term as prefLabel for paren concept.
        List<Identifier> ref;
        if (parentReferences.get("prefLabelXl") != null)
            ref = parentReferences.get("prefLabelXl");
        else
            ref = new ArrayList<>();
        ref.add(new Identifier(node.getId(), typeId));
        parentReferences.put("prefLabelXl", ref);
        return node;
    }

    /**
     * Add individual named attribute to property list
     *
     * @param attributeName like prefLabel
     * @param properties    Propertylist where attribute is added
     * @param att           Attribute to be added
     */
    private void addProperty(String attributeName, Map<String, List<Attribute>> properties, Attribute att) {
        if (!properties.containsKey(attributeName)) {
            List<Attribute> a = new ArrayList<>();
            a.add(att);
            properties.put(attributeName, a);
        } else
            properties.get(attributeName).add(att);
    }

    private GenericNode CreateConcept(GenericNodeInlined vocabulary, ConceptSuggestion incoming,
            Map<String, List<Identifier>> conceptReferences) {
        GenericNode node = null;
        Map<String, List<Attribute>> properties = new HashMap<>();
        addProperty("definition", properties, incoming.getDefinition());
        Attribute att = new Attribute("", "SUGGESTED");
        addProperty("status", properties, att);

        // Create Concept
        TypeId typeId = typeMap.get("Concept").getDomain();
        // Note! Autogenerated UUID
        node = new GenericNode(typeId, properties, conceptReferences);
        return node;
    }

    /**
     * Inner class for vocabulary-UUID pair.
     */
    private class IdCode {

        String code;
        UUID id;

        public IdCode(String code, UUID id) {
            this.code = code;
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

    }
}
