package fi.csc.termed.search.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ObjectUtils {

    @NotNull
    public static <T> T requireNonNull(@Nullable T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }
}
