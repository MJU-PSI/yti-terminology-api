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
import org.springframework.jms.core.BrowserCallback;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.jms.JMSException;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

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
        System.out.println("ReadyState="+getJobState(jobtoken, "VocabularyReady"));
        // Status not_found/running/errors
        // Query status information from ActiveMQ
        Boolean stat = getJobState(jobtoken, "VocabularyStatus");
        System.out.println("Check Status:"+stat.booleanValue());
        stat = getJobState(jobtoken, "VocabularyReady");
        System.out.println("Check ready:"+stat.booleanValue());

        if (getJobState(jobtoken, "VocabularyIncoming").booleanValue()){
            return new ResponseEntity<>("{ \"status\":\"Import operation already started\"}", HttpStatus.NOT_ACCEPTABLE);
        }else if (getJobState(jobtoken, "VocabularyStatus").booleanValue()){
            System.out.println("Status found:");
            return new ResponseEntity<>("{ \"status\":\"Processing\"}", HttpStatus.OK);
        }else if (getJobState(jobtoken, "VocabularyProcessing").booleanValue()){
            return new ResponseEntity<>("{ \"status\":\"Processing\"}", HttpStatus.OK);
        } else if(getJobState(jobtoken, "VocabularyReady").booleanValue()){
            System.out.println("Ready found:");
            return new ResponseEntity<>("{ \"status\":\"Ready\"}", HttpStatus.OK);
        }
        return new ResponseEntity<>("{ \"status\":\"Not found\"}", HttpStatus.OK);
    }

    private Boolean getJobState(UUID jobtoken,String queueName) {
        return jmsMessagingTemplate.getJmsTemplate().browseSelected(queueName, "jobtoken='"+jobtoken.toString()+"'",new BrowserCallback<Boolean>() {
            @Override
            public Boolean doInJms(Session session, QueueBrowser browser) throws JMSException {
                Enumeration messages = browser.getEnumeration();
                return  new Boolean(messages.hasMoreElements());
            }
        });
    }

    private boolean checkIfImportIsRunnig(String uri) {
        Boolean rv = false;
        if (checkUriStatus(uri, "VocabularyStatus").booleanValue()) {
            System.out.println("Status found:");
            rv = true;
        } else if (checkUriStatus(uri, "VocabularyProcessing").booleanValue()) {
            rv = true;
        }
        return rv;
    }

    private Boolean checkUriStatus(String uri, String queueName) {
        return jmsMessagingTemplate.getJmsTemplate().browseSelected(queueName, "uri='"+uri+"'",new BrowserCallback<Boolean>() {
            @Override
            public Boolean doInJms(Session session, QueueBrowser browser) throws JMSException {
                Enumeration messages = browser.getEnumeration();
                return  new Boolean(messages.hasMoreElements());
            }
        });
    }

    ResponseEntity handleNtrfDocumentAsync(String format, UUID vocabularyId, MultipartFile file) {
        String rv;
        System.out.println("Incoming vocabularity= "+vocabularyId+" - file:"+file.getName()+" size:"+file.getSize()+ " type="+file.getContentType());
        // Fail if given format string is not ntrf
        if (!format.equals("ntrf")) {
            logger.error("Unsupported format:<" + format + "> (Currently supported formats: ntrf)");
            // Unsupported format
            ResponseEntity<?> re = new ResponseEntity<>("Unsupported format:<" + format + ">    (Currently supported formats: ntrf)\n", HttpStatus.NOT_ACCEPTABLE);
            return new ResponseEntity<>("Unsupported format:<" + format + ">    (Currently supported formats: ntrf)\n", HttpStatus.NOT_ACCEPTABLE);
        }

        Graph vocabulary = null;

        // Get vocabularity
        try {
            vocabulary = termedService.getGraph(vocabularyId);
            // Import running for given vocabulary, drop it
            if(checkIfImportIsRunnig(vocabulary.getUri())){
                logger.error("Import running for Vocabulary:<" + vocabularyId + ">");
                return new ResponseEntity<>("Import running for Vocabulary:<" + vocabularyId+">", HttpStatus.CONFLICT);
            }
        } catch ( NullPointerException nex){
            // Vocabularity not found
            logger.error("Vocabulary:<" + vocabularyId + "> not found");
            return new ResponseEntity<>("Vocabulary:<" + vocabularyId + "> not found\n", HttpStatus.NOT_FOUND);
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
                    // Authenticated user
                    .setHeader("userId", userProvider.getUser().getId().toString())
                    // Token which is used when querying status
                    .setHeader("jobtoken", operationId.toString())
                    .setHeader("format", "ntrf")
                    // Target vocabulary
                    .setHeader("vocabularyId", vocabularyId.toString())
                    .setHeader("uri", vocabulary.getUri())
                    .build();
            jmsMessagingTemplate.send("VocabularyIncoming", mess);
        } catch (IOException ioe){
            System.out.println("Incoming transform error=" + ioe);
        } catch (XMLStreamException se) {
            System.out.println("Incoming transform error=" + se);
        } catch(JAXBException je){
            System.out.println("Incoming transform error=" + je);
        }
        return new ResponseEntity<>( rv, HttpStatus.OK);
    }
}
