package fi.vm.yti.terminology.api.exception;

import org.springframework.web.client.ResourceAccessException;

public class TermedEndpointException extends RuntimeException {
    public TermedEndpointException(ResourceAccessException e) {
        super("Termed endpoint connection failed", e);
    }
}
