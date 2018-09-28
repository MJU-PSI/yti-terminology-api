package fi.vm.yti.terminology.api.util;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CollectionUtils {

    public static <T> @NotNull List<T> filterToList(@NotNull Collection<T> collection, Predicate<T> predicate) {
        return collection.stream().filter(predicate).collect(Collectors.toList());
    }

    public static <T, R> @NotNull List<R> mapToList(@NotNull Collection<T> collection, Function<T, R> mapper) {
        return collection.stream().map(mapper).collect(Collectors.toList());
    }

    public static <T, R> @NotNull Set<R> mapToSet(@NotNull Collection<T> collection, Function<T, R> mapper) {
        return collection.stream().map(mapper).collect(Collectors.toSet());
    }

    public static <T> @NotNull T requireSingle(@Nullable Collection<T> collection) {

        if (collection == null) {
            throw new RuntimeException("Expecting non-null collection");
        }

        int size = collection.size();

        if (size != 1) {
            throw new RuntimeException("Expecting single, was: " + size);
        }

        return collection.iterator().next();
    }

    private CollectionUtils() {
    }
}
