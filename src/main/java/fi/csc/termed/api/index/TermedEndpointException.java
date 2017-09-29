package fi.csc.termed.api.index;

import java.io.IOException;

public class TermedEndpointException extends RuntimeException {
    TermedEndpointException(IOException e) {
        super("Termed endpoint connection failed", e);
    }
}
