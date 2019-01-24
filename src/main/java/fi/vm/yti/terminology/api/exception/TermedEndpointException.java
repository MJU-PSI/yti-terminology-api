package fi.vm.yti.terminology.api.exception;

import org.springframework.web.client.ResourceAccessException;

public class TermedEndpointException extends RuntimeException {
    private static final long serialVersionUID = 9213550440407869694L;

    public TermedEndpointException(ResourceAccessException e) {
        super("Termed endpoint connection failed", e);
    }
}
