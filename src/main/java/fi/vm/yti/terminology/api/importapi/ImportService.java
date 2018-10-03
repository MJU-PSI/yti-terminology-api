package fi.vm.yti.terminology.api.importapi;

import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.terminology.api.TermedRequester;
import fi.vm.yti.terminology.api.frontend.FrontendGroupManagementService;
import fi.vm.yti.terminology.api.frontend.FrontendTermedService;
import fi.vm.yti.terminology.api.model.ntrf.VOCABULARY;
import fi.vm.yti.terminology.api.model.termed.Graph;
import fi.vm.yti.terminology.api.model.termed.MetaNode;
import fi.vm.yti.terminology.api.security.AuthorizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@EnableJms
public class ImportService {

    private final TermedRequester termedRequester;
    private final FrontendGroupManagementService groupManagementService;
    private final FrontendTermedService termedService;
    private final AuthenticatedUserProvider userProvider;
    private final AuthorizationManager authorizationManager;
    private final String namespaceRoot;

    /**
     * Map containing metadata types. used  when creating nodes.
     */
    private HashMap<String,MetaNode> typeMap = new HashMap<>();

    private Map<UUID,String> importStatus = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);


    // JMS-client
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

    @Autowired
    public ImportService(TermedRequester termedRequester,
                         FrontendGroupManagementService groupManagementService,
                         FrontendTermedService frontendTermedService,
                         AuthenticatedUserProvider userProvider,
                         AuthorizationManager authorizationManager,
                         @Value("${namespace.root}") String namespaceRoot) {
        this.termedRequester = termedRequester;
        this.groupManagementService = groupManagementService;
        this.termedService = frontendTermedService;
        this.userProvider = userProvider;
        this.authorizationManager = authorizationManager;
        this.namespaceRoot = namespaceRoot;
    }

    ResponseEntity getStatus(UUID jobtoken){
        // Status not_found/running/errors
        if(importStatus.get(jobtoken)!= null){
            return new ResponseEntity<>("{ \"status\":\"" + importStatus.get(jobtoken) + "\"" +
                    "}", HttpStatus.OK);
        } else
            return new ResponseEntity<>("{ \"status\":\"" + jobtoken + "\"}", HttpStatus.OK);
    }

    ResponseEntity handleNtrfDocumentAsync(String format, UUID vocabularyId, MultipartFile file) {
        String rv;
        System.out.println("Incoming vocabularity= "+vocabularyId+" - file:"+file.getName()+" size:"+file.getSize()+ " type="+file.getContentType());

        Graph vocabulary = null;

        // Get vocabularity
        try {
            vocabulary = termedService.getGraph(vocabularyId);
        } catch ( NullPointerException nex){
            // Vocabularity not found
            logger.error("Vocabularity:<" + vocabularyId + "> not found");
            return new ResponseEntity<>("Vocabularity:<" + vocabularyId + "> not found\n", HttpStatus.NOT_FOUND);
        }

        UUID operationId=UUID.randomUUID();
        if(!file.getContentType().equalsIgnoreCase("text/xml")){
            rv = "{\"operation\":\""+operationId+"\", \"error\":\"incoming file type  is wrong\"}";
            return new ResponseEntity<>( rv, HttpStatus.BAD_REQUEST);
        }
        rv = "{\"jobtoken\":\"" + operationId + "\"}";
        // Handle incoming cml
        try {
            JAXBContext jc = JAXBContext.newInstance(VOCABULARY.class);
            // Disable DOCTYPE-directive from incoming file.
            XMLInputFactory xif = XMLInputFactory.newFactory();
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader xsr = xif.createXMLStreamReader(file.getInputStream());
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            // At last, resolve ntrf-POJO's
            VOCABULARY voc = (VOCABULARY) unmarshaller.unmarshal(xsr);

            // All set up, execute actual import
            List<?> l = voc.getRECORDAndHEADAndDIAG();
            System.out.println("Incoming objects count=" + l.size());
            importStatus.put(operationId, "Running 0/"+l.size());

            StringWriter sw = new StringWriter();
            marshaller.marshal(voc, sw);
            // Use Vocabulary Uri as uri
            Message mess = MessageBuilder
                    .withPayload(sw.toString())
                    .setHeader("userId", userProvider.getUser().getId().toString()) // Authenticated user
                    .setHeader("jobtoken", operationId.toString())
                    .setHeader("format", "ntrf")
                    .setHeader("vocabularyId", vocabularyId.toString())
                    .setHeader("uri", vocabulary.getUri())
                    .build();
            sendNtrfToJMS(mess);
        } catch (IOException ioe){
            System.out.println("Incoming transform error=" + ioe);
        } catch (XMLStreamException se) {
            System.out.println("Incoming transform error=" + se);
        } catch(JAXBException je){
            System.out.println("Incoming transform error=" + je);
        }
        return new ResponseEntity<>( rv, HttpStatus.OK);
    }

    @SendTo("VocabularyIncoming")
    private void sendNtrfToJMS(Message msg){
        System.out.println("Sending message. Queue="+"VocabularyIncoming");
        System.out.println("Headers:"+msg.getHeaders());
      jmsMessagingTemplate.send("VocabularyIncoming", msg);
//        jmsMessagingTemplate.convertAndSend(
 //               "VocabularyIncoming", msg);
    }
}
