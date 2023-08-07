package fi.vm.yti.terminology.api.importapi.excel;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringValueDTO extends AbstractValueDTO<String> {
    private static final Logger logger = LoggerFactory.getLogger(StringValueDTO.class);

    public StringValueDTO(@NotNull String value) {
        super(value);
    }

    public @NotNull String getValueAsString() {
        return this.value;
    }

    public int getValueAsInt() {
        try {
            return Integer.parseInt(this.value);
        } catch(NumberFormatException e) {
            logger.error("Error parsing number", e);
            return 0;
        }
    }

    public boolean isInteger() {
        try {
            Integer.parseInt(this.value);
        } catch(NumberFormatException e) {
            logger.error("Error parsing number", e);
            return false;
        }

        return true;
    }
}
