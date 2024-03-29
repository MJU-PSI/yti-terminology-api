package fi.vm.yti.terminology.api.config;

import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("datamodel")
@Component
@Validated
public class DatamodelProperties {

    @NotNull
    private Uri uri;

    public Uri getUri() {
        return uri;
    }

    public void setUri(final Uri uri) {
        this.uri = uri;
    }

    public static class Uri {
        @NotNull
        private String scheme;

        @NotNull
        private String host;

        @NotNull
        private String contextPath;

        public String getScheme() {
            return scheme;
        }
    
        public void setScheme(final String scheme) {
            this.scheme = scheme;
        }

        public String getHost() {
            return host;
        }
    
        public void setHost(final String host) {
            this.host = host;
        }

        public String getContextPath() {
            return "/" + this.contextPath.replaceAll("^/|/$", "") + "/";
        }
    
        public void setContextPath(final String contextPath) {
            this.contextPath = contextPath;
        }
    
        public String getUriHostPathAddress() {
            return this.scheme + "://" + this.host + getContextPath();
        }
    }
}

