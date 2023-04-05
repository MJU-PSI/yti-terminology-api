package fi.vm.yti.terminology.api.config;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("uri")
@Component
@Validated
public class UriProperties {

    @NotNull
    private String host;

    @NotNull
    private String scheme;

    @NotNull
    private String contextPath;

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    public String getContextPath() {
        return "/" + this.contextPath.replaceAll("^/|/$", "") + "/";
    }

    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }

    public String getUriHostAddress() {
        return this.scheme + "://" + this.host;
    }

    public String getUriHostPathAddress() {
        return this.scheme + "://" + this.host + this.contextPath;
    }
}

