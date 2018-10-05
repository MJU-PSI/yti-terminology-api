package fi.vm.yti.terminology.api.importapi;

import fi.vm.yti.terminology.api.model.ntrf.VOCABULARY;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.UUID;

@Component
public class ImportJmsListener {
	@Autowired
	NtrfMapper ntrfMapper;
    // JMS-client
    @Autowired
    private JmsMessagingTemplate jmsMessagingTemplate;

    /**
     * State-handler queue, just receive and move it into the actual processing queue
     * @param message
     * @return
     * @throws JMSException
     */
	@JmsListener(destination = "VocabularyIncoming")
	@SendTo("VocabularyProcessing")
	public Message receiveMessage(final Message message,
                                  Session session,
                                  @Header String jobtoken,
                                  @Header String userId,
                                  @Header String format,
                                  @Header String vocabularyId,
                                  @Header String uri) throws JMSException {
        System.out.println("Received and transferred to processing. message headers="+message.getHeaders());
        // Send status message
        Message mess = MessageBuilder
                .withPayload("Processing "+ uri)
                // Authenticated user
                .setHeader("userId", userId)
                // Token which is used when querying status
                .setHeader("jobtoken", jobtoken)
                .setHeader("format",format)
                // Target vocabulary
                .setHeader("vocabularyId", vocabularyId)
                .setHeader("uri", uri)
                .build();
        jmsMessagingTemplate.send("VocabularyStatus",mess);
        return message;
	}

    @JmsListener(id="NtrfProcessor", destination = "VocabularyProcessing")
	@SendTo("VocabularyReady")
	public Message processMessage(final Message message,Session session,
                                 @Header String jobtoken,
                                 @Header String userId,
                                 @Header String format,
                                 @Header String vocabularyId,
                                 @Header String uri) throws JMSException {
	    // Consume incoming
        System.out.println("Process message "+ message.getHeaders());
        System.out.println("session= "+ session);
        System.out.println("UserId="+userId);
        String payload ="{}";
        if(jmsMessagingTemplate == null){
            System.out.println("MessagingTemplate not initialized!!!!!");
        }
        // Process ntrf item
        try {
            JAXBContext jc = JAXBContext.newInstance(VOCABULARY.class);
            // Disable DOCTYPE-directive from incoming file.
            XMLInputFactory xif = XMLInputFactory.newFactory();
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            // Unmarshall XMl with JAXB
            Reader inReader = new StringReader((String)message.getPayload());
            XMLStreamReader xsr = xif.createXMLStreamReader(inReader);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            // At last, resolve ntrf-POJO's
            VOCABULARY voc = (VOCABULARY) unmarshaller.unmarshal(xsr);
            payload=ntrfMapper.mapNtrfDocument( UUID.fromString(vocabularyId), voc,UUID.fromString(userId));
        } catch (XMLStreamException se) {
            System.out.println("Incoming transform error=" + se);
        } catch(JAXBException je){
            System.out.println("Incoming transform error=" + je);
        }
        // Set import as handled. IE. consume processed message
        setReady(jobtoken,"VocabularyStatus");
        // Set result as a payload and move it to ready-queue
        Message mess = MessageBuilder
                .withPayload(payload)
                // Authenticated user
                .setHeader("userId", userId)
                // Token which is used when querying status
                .setHeader("jobtoken", jobtoken)
                .setHeader("format",format)
                // Target vocabulary
                .setHeader("vocabularyId", vocabularyId)
                .setHeader("uri", uri)
                .build();
        return mess;
	}

    private void setReady(String jobtoken, String queueName) {
        System.out.println("Consume Processed item:");
        try {
            javax.jms.Message m = jmsMessagingTemplate.getJmsTemplate().receiveSelected(queueName, "jobtoken='" + jobtoken.toString() + "'");
            System.out.println("Deleting "+m.getStringProperty("jobtoken"));
        } catch (JMSException jex) {
            jex.printStackTrace();
        }
    }
}