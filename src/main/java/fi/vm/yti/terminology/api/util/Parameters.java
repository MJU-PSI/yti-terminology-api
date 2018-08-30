package fi.vm.yti.terminology.api.util;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class Parameters {

    private final List<NameValuePair> parameters = new ArrayList<>();

    public static @NotNull Parameters empty() {
        return new Parameters();
    }

    public static @NotNull Parameters single(@NotNull String name, @NotNull String value) {
        Parameters result = new Parameters();
        result.add(name, value);
        return result;
    }

    public void add(@NotNull String name, @NotNull String value) {
        this.parameters.add(new BasicNameValuePair(name, value));
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> toMap() {
        return this.parameters.stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    public String toEncodedString() {
        return this.toString(true);
    }

    private String toString(boolean encode) {

        StringBuilder result = new StringBuilder();

        if (!parameters.isEmpty()) {
            result.append("?");
            result.append(
                    parameters.stream()
                            .map(param -> param.getName() + "=" + (encode ? urlEncode(param.getValue()) : param.getValue()))
                            .collect(joining("&")));
        }

        return result.toString();
    }
}