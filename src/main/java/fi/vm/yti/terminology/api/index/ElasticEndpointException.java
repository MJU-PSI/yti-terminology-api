package fi.vm.yti.terminology.api.index;

import java.io.IOException;

public class ElasticEndpointException extends RuntimeException {
    ElasticEndpointException(IOException e) {
        super("Elastic endpoint connection failed", e);
    }
}
