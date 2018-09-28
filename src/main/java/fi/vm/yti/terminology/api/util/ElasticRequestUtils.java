package fi.vm.yti.terminology.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.Response;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public final class ElasticRequestUtils {

    private ElasticRequestUtils() {
        // prevent construction
    }

    public static @NotNull JsonNode responseContentAsJson(@NotNull ObjectMapper objectMapper,
                                                          @NotNull Response response) {
        try {
            return objectMapper.readTree(response.getEntity().getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NotNull String responseContentAsString(@NotNull Response response) {
        try (InputStream is = response.getEntity().getContent()) {
            return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
