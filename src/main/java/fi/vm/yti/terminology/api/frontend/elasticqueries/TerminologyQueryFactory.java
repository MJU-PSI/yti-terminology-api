package fi.vm.yti.terminology.api.frontend.elasticqueries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.elasticsearch.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.terminology.api.frontend.searchdto.DeepSearchHitListDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.InformationDomainDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.OrganizationDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologyDTO;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchRequest;
import fi.vm.yti.terminology.api.frontend.searchdto.TerminologySearchResponse;
import fi.vm.yti.terminology.api.util.ElasticRequestUtils;

public class TerminologyQueryFactory {

    private static final Logger log = LoggerFactory.getLogger(DeepConceptQueryFactory.class);

    private static final String deep0 =
        "{\n" +
            "  \"query\" : {\n" +
            "    \"bool\" : {\n" +
            "      \"should\" : [ {\n" +
            "        \"bool\" : {\n" +
            "          \"must\": [ {\n" +
            "            \"match_phrase_prefix\" : {\n" +
            "              \"properties.prefLabel.value\" : {\n" +
            "                \"query\": \"";
    private static final String deep1 =
        "\"\n" +
            "              }\n" +
            "            }\n" +
            "          } ],\n" +
            "          \"must_not\" : []\n" +
            "        }\n" +
            "      }, {\n" +
            "        \"terms\" : {\n" +
            "          \"type.graph.id.keyword\" : [";
    private static final String deep2 =
        "]\n" +
            "        }\n" +
            "      } ],\n" +
            "      \"minimum_should_match\" : 1\n" +
            "    }\n" +
            "  },\n" +
            "  \"size\" : ";
    private static final String deep3 = ",\n" +
        "  \"from\" : ";
    private static final String deep4 = "\n" +
        "}\n";
    private static final String part0 =
        "{\n" +
            "  \"query\" : {\n" +
            "    \"bool\" : {\n" +
            "      \"must\": [ {\n" +
            "        \"match_phrase_prefix\" : {\n" +
            "          \"properties.prefLabel.value\" : {\n" +
            "            \"query\": \"";
    private static final String part1 =
        "\"\n" +
            "          }\n" +
            "        }\n" +
            "      } ],\n" +
            "      \"must_not\" : []\n" +
            "    }\n" +
            "  },\n" +
            "  \"size\" : ";
    private static final String part2 = ",\n" +
        "  \"from\" : ";
    private static final String part3 = "\n" +
        "}";

    private ObjectMapper objectMapper;

    public TerminologyQueryFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String createQuery(TerminologySearchRequest request) {
        return createQuery(request.getQuery(), request.getPageSize(), request.getPageFrom());
    }

    public String createQuery(TerminologySearchRequest request,
                              Collection<String> additionalTerminologyIds) {
        if (additionalTerminologyIds.isEmpty()) {
            return createQuery(request.getQuery(), request.getPageSize(), request.getPageFrom());
        }
        return createQuery(request.getQuery(), additionalTerminologyIds, request.getPageSize(), request.getPageFrom());
    }

    private String createQuery(String query,
                               Collection<String> additionalTerminologyIds,
                               int pageSize,
                               int pageFrom) {
        StringBuilder sb = new StringBuilder(deep0);
        JsonStringEncoder.getInstance().quoteAsString(query, sb);
        sb.append(deep1);
        sb.append(additionalTerminologyIds.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",")));
        sb.append(deep2);
        sb.append(pageSize);
        sb.append(deep3);
        sb.append(pageFrom);
        sb.append(deep4);
        return sb.toString();
    }

    private String createQuery(String query,
                               int pageSize,
                               int pageFrom) {
        if (query.isEmpty()) {
            return createQuery(pageSize, pageFrom);
        }

        StringBuilder sb = new StringBuilder(part0);
        JsonStringEncoder.getInstance().quoteAsString(query, sb);
        sb.append(part1);
        sb.append(pageSize);
        sb.append(part2);
        sb.append(pageFrom);
        sb.append(part3);
        return sb.toString();
    }

    private String createQuery(int pageSize,
                               int pageFrom) {
        return
            "{\n" +
                "  \"query\" : {\n" +
                "    \"match_all\" : {}\n" +
                "  },\n" +
                "  \"size\" : " + pageSize + ",\n" +
                "  \"from\" : " + pageFrom + "\n" +
                "}\n";
    }

    public TerminologySearchResponse parseResponse(Response response,
                                                   TerminologySearchRequest request,
                                                   Map<String, List<DeepSearchHitListDTO<?>>> deepSearchHitList) {
        List<TerminologyDTO> terminologies = new ArrayList<>();
        TerminologySearchResponse ret = new TerminologySearchResponse(0, request.getPageFrom(), terminologies, deepSearchHitList);
        try {
            JsonNode root = objectMapper.readTree(response.getEntity().getContent());
            JsonNode meta = root.get("hits");
            ret.setTotalHitCount(meta.get("total").intValue());
            JsonNode hits = meta.get("hits");
            for (JsonNode hit : hits) {
                JsonNode terminology = hit.get("_source");
                // NOTE: terminology.get("id") would make more sense, but currently concepts contain only graph id => use it here also.
                String terminologyId = terminology.get("type").get("graph").get("id").textValue();
                String terminologyCode = ElasticRequestUtils.getTextValueOrNull(terminology, "code");
                String terminologyUri = ElasticRequestUtils.getTextValueOrNull(terminology, "uri");

                JsonNode properties = terminology.get("properties");
                JsonNode statusArray = properties.get("status");
                String terminologyStatus = statusArray != null ? (statusArray.has(0) ? statusArray.get(0).get("value").textValue() : "DRAFT") : "DRAFT";
                Map<String, String> labelMap = ElasticRequestUtils.labelFromLangValueArray(properties.get("prefLabel"));
                Map<String, String> descriptionMap = ElasticRequestUtils.labelFromLangValueArray(properties.get("description"));

                JsonNode references = terminology.get("references");
                JsonNode domainArray = references.get("inGroup");
                JsonNode contributorArray = references.get("contributor");
                List<InformationDomainDTO> domains = new ArrayList<>();
                List<OrganizationDTO> contributors = new ArrayList<>();
                if (domainArray != null) {
                    for (JsonNode domain : domainArray) {
                        String domainId = domain.get("id").textValue();
                        Map<String, String> domainLabel = ElasticRequestUtils.labelFromLangValueArray(domain.get("properties").get("prefLabel"));
                        domains.add(new InformationDomainDTO(domainId, domainLabel));
                    }
                }
                if (contributorArray != null) {
                    for (JsonNode contributor : contributorArray) {
                        String orgId = contributor.get("id").textValue();
                        Map<String, String> orgLabel = ElasticRequestUtils.labelFromLangValueArray(contributor.get("properties").get("prefLabel"));
                        contributors.add(new OrganizationDTO(orgId, orgLabel));
                    }
                }

                terminologies.add(new TerminologyDTO(terminologyId, terminologyCode, terminologyUri, terminologyStatus, labelMap, descriptionMap, domains, contributors));

            }
        } catch (Exception e) {
            log.error("Cannot parse terminology query response", e);
        }
        return ret;
    }
}
