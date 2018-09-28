package fi.vm.yti.terminology.api.exception;

import java.io.IOException;

public class ElasticEndpointException extends RuntimeException {
    public ElasticEndpointException(IOException e) {
        super("Elastic endpoint connection failed", e);
    }
}
