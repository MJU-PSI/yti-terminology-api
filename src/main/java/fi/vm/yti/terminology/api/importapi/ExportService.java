package fi.vm.yti.terminology.api.importapi;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.terminology.api.TermedContentType;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.frontend.FrontendGroupManagementService;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.importapi.ImportStatusResponse.Status;
import fi.vm.yti.terminology.api.model.ntrf.VOCABULARY;
import fi.vm.yti.terminology.api.model.termed.Graph;
import fi.vm.yti.terminology.api.model.termed.MetaNode;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import fi.vm.yti.terminology.api.util.JsonUtils;
import fi.vm.yti.terminology.api.util.Parameters;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.TerminologicalVocabulary;
import static fi.vm.yti.terminology.api.model.termed.VocabularyNodeType.Vocabulary;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@EnableJms
public class ExportService {

    private final TermedRequester termedRequester;
    private final FrontendGroupManagementService groupManagementService;
    private final AuthenticatedUserProvider userProvider;
    private final AuthorizationManager authorizationManager;

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    @Autowired
    public ExportService(TermedRequester termedRequester,
                         FrontendGroupManagementService groupManagementService,
                         AuthenticatedUserProvider userProvider,
                         AuthorizationManager authorizationManager) {
        this.termedRequester = termedRequester;
        this.groupManagementService = groupManagementService;
        this.userProvider = userProvider;
        this.authorizationManager = authorizationManager;
    }

    @NotNull JsonNode getFullVocabulary(UUID id) {
        /*
        https://sanastot-dev.suomi.fi/termed-api/graphs/5b3eb5d7-0239-484d-8515-bc4b8cb42e7e/node-trees?select=*,references.prefLabelXl:1&where=type.id:Concept%20OR%20type.id:Collection&max=-1

        */

        Parameters params = new Parameters();
        params.add("select", "*");
        params.add("select", "references.prefLabelXl:1");
        // Get all nodes from given graph
//        params.add("where", "graph.id:" + id + " AND (type.id:" + Vocabulary + " OR type.id:"
//        + TerminologicalVocabulary + ")");
        params.add("where", "graph.id:" + id );

        params.add("max", "-1");
        System.out.println("getFull vocabulary: "+params);

        // Execute full search
        JsonNode rv =  requireNonNull(termedRequester.exchange("/node-trees", GET, params, JsonNode.class));
        JsonUtils.prettyPrintJson(rv);
        return requireNonNull(rv);            
    }
        
    @NotNull String getFullVocabularyRDF(UUID id) {
        /*
        https://sanastot-dev.suomi.fi/termed-api/graphs/5b3eb5d7-0239-484d-8515-bc4b8cb42e7e/node-trees?select=*,references.prefLabelXl:1&where=type.id:Concept%20OR%20type.id:Collection&max=-1

        */

        Parameters params = new Parameters();
        params.add("select", "*");
        params.add("select", "references.prefLabelXl:1");
        // Get all nodes from given graph
//        params.add("where", "graph.id:" + id + " AND (type.id:" + Vocabulary + " OR type.id:"
//        + TerminologicalVocabulary + ")");
        params.add("where", "graph.id:" + id );

        params.add("max", "-1");
        params.add("content-type","application/rdf+xml");
        System.out.println("getFullRDF vocabulary: "+params);

        // Execute full search        
        String rv =  requireNonNull(termedRequester.exchange("/node-trees", GET, params, String.class,TermedContentType.RDF_XML));
        System.out.println("RDF-XML="+rv);
        return requireNonNull(rv);            
    }

    ResponseEntity getJSON(UUID vocabularyId){
        // Query status information from ActiveMQ
        System.out.println("getJSON: "+vocabularyId.toString());
        System.out.println("Response status json");

        JsonNode response = getFullVocabulary(vocabularyId);
        // Construct return message
        return new ResponseEntity<>(JsonUtils.prettyPrintJsonAsString(response), HttpStatus.OK);
    }

    ResponseEntity getRDF(UUID vocabularyId){
        // Query status information from ActiveMQ
        HttpStatus status;
        StringBuffer statusString= new StringBuffer();
        System.out.println("Response status RDF");
        String response = getFullVocabularyRDF(vocabularyId);

        // Construct return message
//        JsonUtils.prettyPrintJson(response);

        return new ResponseEntity<>("{}", HttpStatus.OK);
//        return new ResponseEntity<>(JsonUtils.prettyPrintJsonAsString(response), HttpStatus.OK);
    }

}
