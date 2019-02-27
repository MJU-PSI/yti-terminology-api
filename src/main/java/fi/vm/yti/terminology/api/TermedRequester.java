package fi.vm.yti.terminology.api;

import fi.vm.yti.terminology.api.exception.TermedEndpointException;
import fi.vm.yti.terminology.api.util.Parameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.function.Supplier;

@Service
public class TermedRequester {

    private static TermedContentType DEFAULT_CONTENT_TYPE = TermedContentType.JSON;

    private final String termedUser;
    private final String termedPassword;
    public final String termedUrl;
    private final RestTemplate restTemplate;

    @Autowired
    TermedRequester(@Value("${api.user}") String termedUser,
                    @Value("${api.pw}") String termedPassword,
                    @Value("${api.url}") String termedUrl,
                    RestTemplate restTemplate) {
        this.termedUser = termedUser;
        this.termedPassword = termedPassword;
        this.termedUrl = termedUrl;
        this.restTemplate = restTemplate;
    }

    public <TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                    @NotNull HttpMethod method,
                                                    @NotNull Parameters parameters,
                                                    @NotNull Class<TResponse> responseType) {
        return exchange(path, method, parameters, responseType, null, termedUser, termedPassword, DEFAULT_CONTENT_TYPE);
    }

    public <TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                    @NotNull HttpMethod method,
                                                    @NotNull Parameters parameters,
                                                    @NotNull Class<TResponse> responseType,
                                                    @NotNull TermedContentType contentType) {
        return exchange(path, method, parameters, responseType, null, termedUser, termedPassword, contentType);
    }

    public <TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                    @NotNull HttpMethod method,
                                                    @NotNull Parameters parameters,
                                                    @NotNull ParameterizedTypeReference<TResponse> responseType) {
        return exchange(path, method, parameters, responseType, null, termedUser, termedPassword, DEFAULT_CONTENT_TYPE);
    }

    public <TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                    @NotNull HttpMethod method,
                                                    @NotNull Parameters parameters,
                                                    @NotNull ParameterizedTypeReference<TResponse> responseType,
                                                    @NotNull TermedContentType contentType) {
        return exchange(path, method, parameters, responseType, null, termedUser, termedPassword, contentType);
    }

    public <TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                    @NotNull HttpMethod method,
                                                    @NotNull Parameters parameters,
                                                    @NotNull Class<TResponse> responseType,
                                                    @NotNull String username,
                                                    @NotNull String password) {
        return exchange(path, method, parameters, responseType, null, username, password, DEFAULT_CONTENT_TYPE);
    }

    public <TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                    @NotNull HttpMethod method,
                                                    @NotNull Parameters parameters,
                                                    @NotNull Class<TResponse> responseType,
                                                    @NotNull String username,
                                                    @NotNull String password,
                                                    @NotNull TermedContentType contentType) {
        return exchange(path, method, parameters, responseType, null, username, password, contentType);
    }

    public <TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                    @NotNull HttpMethod method,
                                                    @NotNull Parameters parameters,
                                                    @NotNull ParameterizedTypeReference<TResponse> responseType,
                                                    @NotNull String username,
                                                    @NotNull String password) {
        return exchange(path, method, parameters, responseType, null, username, password, DEFAULT_CONTENT_TYPE);
    }

    public <TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                    @NotNull HttpMethod method,
                                                    @NotNull Parameters parameters,
                                                    @NotNull ParameterizedTypeReference<TResponse> responseType,
                                                    @NotNull String username,
                                                    @NotNull String password,
                                                    @NotNull TermedContentType contentType) {
        return exchange(path, method, parameters, responseType, null, username, password, contentType);
    }

    public <TRequest, TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                              @NotNull HttpMethod method,
                                                              @NotNull Parameters parameters,
                                                              @NotNull Class<TResponse> responseType,
                                                              @Nullable TRequest body) {
        return exchange(path, method, parameters, responseType, body, termedUser, termedPassword, DEFAULT_CONTENT_TYPE);
    }

    public <TRequest, TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                              @NotNull HttpMethod method,
                                                              @NotNull Parameters parameters,
                                                              @NotNull Class<TResponse> responseType,
                                                              @Nullable TRequest body,
                                                              @NotNull TermedContentType contentType) {
        return exchange(path, method, parameters, responseType, body, termedUser, termedPassword, contentType);
    }

    public <TRequest, TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                              @NotNull HttpMethod method,
                                                              @NotNull Parameters parameters,
                                                              @NotNull ParameterizedTypeReference<TResponse> responseType,
                                                              @Nullable TRequest body) {
        return exchange(path, method, parameters, responseType, body, termedUser, termedPassword, DEFAULT_CONTENT_TYPE);
    }

    public <TRequest, TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                              @NotNull HttpMethod method,
                                                              @NotNull Parameters parameters,
                                                              @NotNull ParameterizedTypeReference<TResponse> responseType,
                                                              @Nullable TRequest body,
                                                              @NotNull TermedContentType contentType) {
        return exchange(path, method, parameters, responseType, body, termedUser, termedPassword, contentType);
    }

    public <TRequest, TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                              @NotNull HttpMethod method,
                                                              @NotNull Parameters parameters,
                                                              @NotNull Class<TResponse> responseType,
                                                              @Nullable TRequest body,
                                                              @NotNull String username,
                                                              @NotNull String password) {
        return exchange(path, method, parameters, responseType, body, username, password, DEFAULT_CONTENT_TYPE);
    }

    public <TRequest, TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                              @NotNull HttpMethod method,
                                                              @NotNull Parameters parameters,
                                                              @NotNull Class<TResponse> responseType,
                                                              @Nullable TRequest body,
                                                              @NotNull String username,
                                                              @NotNull String password,
                                                              @NotNull TermedContentType contentType) {
        return mapExceptions(() -> restTemplate.exchange(createUrl(path, parameters), method, new HttpEntity<>(body, createHeaders(username, password, contentType)), responseType).getBody());
    }

    public <TRequest, TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                              @NotNull HttpMethod method,
                                                              @NotNull Parameters parameters,
                                                              @NotNull ParameterizedTypeReference<TResponse> responseType,
                                                              @Nullable TRequest body,
                                                              @NotNull String username,
                                                              @NotNull String password) {
        return exchange(path, method, parameters, responseType, body, username, password, DEFAULT_CONTENT_TYPE);
    }

    public <TRequest, TResponse> @Nullable TResponse exchange(@NotNull String path,
                                                              @NotNull HttpMethod method,
                                                              @NotNull Parameters parameters,
                                                              @NotNull ParameterizedTypeReference<TResponse> responseType,
                                                              @Nullable TRequest body,
                                                              @NotNull String username,
                                                              @NotNull String password,
                                                              @NotNull TermedContentType contentType) {
        return mapExceptions(() -> restTemplate.exchange(createUrl(path, parameters), method, new HttpEntity<>(body, createHeaders(username, password, contentType)), responseType).getBody());
    }

    private static <T> T mapExceptions(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (ResourceAccessException e) {
            throw new TermedEndpointException(e);
        } catch (HttpClientErrorException ex)   {
            if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw ex;
            } else {
                return null;
            }
        }
    }

    private @NotNull HttpHeaders createHeaders(String username, String password, TermedContentType contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, createAuthorizationHeaderValue(username, password));
        headers.add(HttpHeaders.ACCEPT, contentType.getContentType());
        return headers;
    }

    private @NotNull String createAuthorizationHeaderValue(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    private @NotNull String createUrl(@NotNull String path, @NotNull Parameters parameters) {
        return termedUrl + path + parameters.toString();
    }
}
