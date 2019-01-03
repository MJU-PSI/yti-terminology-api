package fi.vm.yti.terminology.api.integration;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.frontend.FrontendGroupManagementService;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.model.integration.ConceptSuggestion;
import fi.vm.yti.terminology.api.model.termed.Attribute;
import fi.vm.yti.terminology.api.model.termed.GenericDeleteAndSave;
import fi.vm.yti.terminology.api.model.termed.GenericNode;
import fi.vm.yti.terminology.api.model.termed.GenericNodeInlined;
import fi.vm.yti.terminology.api.model.termed.Graph;
import fi.vm.yti.terminology.api.model.termed.Identifier;
import fi.vm.yti.terminology.api.model.termed.MetaNode;
import fi.vm.yti.terminology.api.model.termed.Property;
import fi.vm.yti.terminology.api.model.termed.TypeId;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import fi.vm.yti.terminology.api.util.JsonUtils;
import fi.vm.yti.terminology.api.exception.NodeNotFoundException;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.text.SimpleDateFormat;

import fi.vm.yti.terminology.api.integration.containers.*;

@Service
public class IntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationService.class);
    private final TermedRequester termedRequester;
    private final FrontendGroupManagementService groupManagementService;
    private final FrontendTermedService termedService;
    private final AuthenticatedUserProvider userProvider;
    private final AuthorizationManager authorizationManager;
    private final String namespaceRoot;
    /**
     * Map containing metadata types. used when creating nodes.
     */
    private HashMap<String, MetaNode> typeMap = new HashMap<>();

    @Autowired
    public IntegrationService(TermedRequester termedRequester, FrontendGroupManagementService groupManagementService,
            FrontendTermedService frontendTermedService, AuthenticatedUserProvider userProvider,
            AuthorizationManager authorizationManager, @Value("${namespace.root}") String namespaceRoot) {
        this.termedRequester = termedRequester;
        this.groupManagementService = groupManagementService;
        this.termedService = frontendTermedService;
        this.userProvider = userProvider;
        this.authorizationManager = authorizationManager;
        this.namespaceRoot = namespaceRoot;
    }

    ResponseEntity handleContainers(String language, int pageSize, int from, String statusEnum, String after,
            boolean includeMeta) {
        if (logger.isDebugEnabled())
            logger.debug("GET /containers requested. status=" + statusEnum);

        // Concept reference-map
        Map<String, List<Identifier>> conceptReferences = new HashMap<>();

        // Response item list
        List<ContainersResponse> resp = new ArrayList<>();
        // Get vocabularies and match code with name
        List<Graph> vocs = termedService.getGraphs();
        System.out.println(" lan=" + language + " pageSize=" + pageSize + " From=" + from + " Status=" + statusEnum
                + " After=" + after + " includeMeta=" + includeMeta);
        vocs.forEach(o -> {
            boolean addItem = true;
            List<Attribute> description = null;
            fi.vm.yti.terminology.api.integration.containers.Description desc = null;
            List<Attribute> status = null;
            Date modifiedDate = null;
            PrefLabel prefLabel = null;
            System.out.println(" vocabulary=" + o.getId().toString() + " URI=" + o.getUri());
            String uri = o.getUri();
            List<Property> preflabels = o.getProperties().get("prefLabel");
            if (preflabels != null) {
                prefLabel = new PrefLabel();
                for (Property p : preflabels) {
                    if ("fi".equalsIgnoreCase(p.getLang())) {
                        prefLabel.setFi(p.getValue());
                    }
                    if ("sv".equalsIgnoreCase(p.getLang())) {
                        prefLabel.setSv(p.getValue());
                    }
                    if ("en".equalsIgnoreCase(p.getLang())) {
                        prefLabel.setEn(p.getValue());
                    }
                }
                ;
            }
            /*
             * { "uri": "http://uri.suomi.fi/codelist/test/010", "prefLabel": { "fi": "010"
             * }, "description": {}, "status": "DRAFT", "modified":
             * "2018-11-28T10:48:09.485Z" }
             */
            try {
                GenericNodeInlined gn = termedService.getVocabulary(o.getId());
                if (gn != null) {
                    description = gn.getProperties().get("description");
                    status = gn.getProperties().get("status");

                    if (description != null) {
                        desc = new Description();
                        for (Attribute at : description) {
                            System.out.println(" desc=" + at.getValue() + " lang=" + at.getLang());
                            if ("fi".equalsIgnoreCase(at.getLang())) {
                                desc.setFi(at.getValue());
                            }
                            if ("sv".equalsIgnoreCase(at.getLang())) {
                                desc.setSv(at.getValue());
                            }
                            if ("en".equalsIgnoreCase(at.getLang())) {
                                desc.setEn(at.getValue());
                            }
                        }
                        ;
                    } else {
                        System.out.println(" description not found:" + gn.getProperties());
                    }
                    // System.out.println("status="+gn.getProperties().get(Status));
                    modifiedDate = gn.getLastModifiedDate();
                    System.out.println("modifiedDate=" + gn.getLastModifiedDate());
                }
            } catch (NodeNotFoundException ex) {
                // System.out.println(ex);
            }

            System.out.println("-------------\nCombined data!");
            System.out.println("URI=" + uri + " \n modifDate=" + modifiedDate);
            List<String> stat = new ArrayList<>();
            if (status != null) {
                status.forEach(p -> {
                    System.out.println(" status=" + p.getValue() + " lang=" + p.getLang());
                    stat.add(p.getValue());
                });
            }
            if (description != null) {
                description.forEach(at -> {
                    System.out.println(" desc=" + at.getValue() + " lang=" + at.getLang());
                });
            }
            ContainersResponse respItem = new ContainersResponse();
            respItem.setUri(uri);
            if (modifiedDate != null) {
                SimpleDateFormat sm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                respItem.setModified(sm.format(modifiedDate));
            }
            if (prefLabel != null) {
                respItem.setPrefLabel(prefLabel);
            }
            if (desc != null) {
                respItem.setDescription(desc);
            }
            if (stat != null && !stat.isEmpty()) {
                System.out.println("Status size=" + stat.size());
                respItem.setStatus(stat.get(0));
            }
            // Filter using status
            if (statusEnum != null) {
                if(respItem.getStatus() == null){
                    addItem = false;
                } else {
                    if (!statusEnum.toUpperCase().contains(respItem.getStatus().toUpperCase())) {
                        addItem = false;
                        System.out.println("Filter out status:" + respItem.getStatus());
                    }
                }
            }
            // filter out vocabularies without uri
            if (uri == null || (uri != null && uri.isEmpty())) {
                addItem = false;
            }

            if (addItem) {
                resp.add(respItem);
            }
            System.out.println("-------------");
        });
        /*
         * // Filter given code as result List<IdCode> vocabularies =
         * vocs.stream().filter(o -> o.getCode().equalsIgnoreCase(vocabularityId)).map(o
         * -> { return new IdCode(o.getCode(), o.getId());
         * }).collect(Collectors.toList());
         * 
         */
        // return new
        // ResponseEntity<>(JsonUtils.prettyPrintJsonAsString(incomingConcept),
        // HttpStatus.OK);
        return new ResponseEntity<>(JsonUtils.prettyPrintJsonAsString(resp), HttpStatus.OK);
    }

    /**
     * Initialize cached META-model. - Read given vocabularity for meta-types, cache
     * them - Read all existing nodes and cache their URI/UUID-values
     *
     * @param vocabularityId UUID of the vocabularity
     */
    private void initImport(UUID vocabularityId) {
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
    ResponseEntity handleConceptSuggestion(String vocabularityId, ConceptSuggestion incomingConcept) {
        if (logger.isDebugEnabled())
            logger.debug("POST /vocabulary/{vocabularyId}/concept requested. creating Concept for "
                    + JsonUtils.prettyPrintJsonAsString(incomingConcept));
        UUID activeVocabulary;
        // Concept reference-map
        Map<String, List<Identifier>> conceptReferences = new HashMap<>();

        // Get vocabularies and match code with name
        List<Graph> vocs = termedService.getGraphs();
        // Filter given code as result
        List<IdCode> vocabularies = vocs.stream().filter(o -> o.getCode().equalsIgnoreCase(vocabularityId)).map(o -> {
            return new IdCode(o.getCode(), o.getId());
        }).collect(Collectors.toList());
        if (vocabularies.size() > 1) {
            return new ResponseEntity<>(
                    "Created Concept suggestion failed for " + vocabularityId + ". Multiple matches for vocabulary. \n",
                    HttpStatus.NOT_FOUND);
        } else if (vocabularies.size() == 1) {
            // found, set UUID
            activeVocabulary = vocabularies.get(0).id;
        } else {
            // It may be UUID, so try to convert that
            // Try if it is UUID
            try {
                activeVocabulary = UUID.fromString(vocabularityId);
            } catch (IllegalArgumentException ex) {
                // Not UUID, error.
                return new ResponseEntity<>(
                        "Created Concept suggestion failed for " + vocabularityId + ". Vocabulary not found. \n",
                        HttpStatus.NOT_FOUND);
            }
        }

        // Try to fetch it just to ensure it exist
        GenericNodeInlined vocabularyNode = termedService.getVocabulary(activeVocabulary);
        if (vocabularyNode == null) {
            return new ResponseEntity<>(
                    "Created Concept suggestion failed for UUID:" + vocabularityId + ". Vocabulary not found. \n",
                    HttpStatus.NOT_FOUND);
        }

        // get metamodel for vocabulary
        initImport(activeVocabulary);
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
        UUID termId = UUID.randomUUID();
        String code = termId.toString();

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
        UUID conceptId = UUID.randomUUID();
        String code = conceptId.toString();
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
