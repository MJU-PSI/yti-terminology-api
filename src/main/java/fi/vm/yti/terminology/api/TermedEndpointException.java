package fi.vm.yti.terminology.api;

import org.springframework.web.client.ResourceAccessException;

class TermedEndpointException extends RuntimeException {
    TermedEndpointException(ResourceAccessException e) {
        super("Termed endpoint connection failed", e);
    }
}
