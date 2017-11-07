package fi.vm.yti.terminology.api.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CollectionUtils {

    public static <T, R> @NotNull List<R> mapToList(@NotNull Collection<T> collection, Function<T, R> mapper) {
        return collection.stream().map(mapper).collect(Collectors.toList());
    }

    private CollectionUtils() {
    }
}
