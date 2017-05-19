package fi.csc.termed.search.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ObjectUtils {

    private ObjectUtils() {
        // prevent construction
    }

    @NotNull
    public static <T> T requireNonNull(@Nullable T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }
}
