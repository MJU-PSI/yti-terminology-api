package fi.vm.yti.terminology.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -4000900590978149488L;

    public ResourceNotFoundException(String prefix, String name) {
        super("Resource not found with prefix: " + prefix + " and name: " + name);
    }
}
