package fi.vm.yti.terminology.api.system;

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

    ResponseEntity<String> countStatistics() {

        if (logger.isDebugEnabled()) {
            logger.debug("GET /count requested.");
        }

        int terminologies = countTerminologies();
        int concepts = countConcepts();
        return new ResponseEntity<>("{ \"terminologyCount\":" + terminologies + ", \"conceptCount\":" + concepts + " }",
            HttpStatus.OK);
    }

    private int countTerminologies() {
        int rv = 0;
        String url = "/node-count?where=type.id:TerminologicalVocabulary";
        String count = termedRequester.exchange(url, GET, Parameters.empty(), String.class);
        logger.info("countTerminologies rv="+count);
        if(count != null){
            rv = Integer.parseInt(count);
        }
        return rv;
    }

    private int countConcepts() {
        int rv = 0;
        String url = "/node-count/?wheretype.id:Concept";
        String count = termedRequester.exchange(url, GET, Parameters.empty(), String.class);
        logger.info("countConcepts rv="+count);
        if(count != null){
            rv = Integer.parseInt(count);
        }
        return rv;
    }
}
