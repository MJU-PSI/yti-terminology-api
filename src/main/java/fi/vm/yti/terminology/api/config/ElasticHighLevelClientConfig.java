package fi.vm.yti.terminology.api.config;

import fi.vm.yti.terminology.api.util.RestHighLevelClientWrapper;
import fi.vm.yti.terminology.api.util.RestHighLevelClientWrapperImpl;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
@PropertySource(value = "classpath", ignoreResourceNotFound = true)
public class ElasticHighLevelClientConfig {

    private static final int ES_CONNECTION_TIMEOUT = 300000;

    @Value("${elasticsearch.scheme}")
    private String elasticsearchScheme;

    @Value("${elasticsearch.host}")
    protected String elasticsearchHost;

    @Value("${elasticsearch.port}")
    protected Integer elasticsearchPort;

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        loggingFilter.setIncludeClientInfo(true);
        loggingFilter.setIncludeQueryString(true);
        loggingFilter.setIncludePayload(true);
        return loggingFilter;
    }

    @Bean
    @SuppressWarnings("resource")
    protected RestHighLevelClientWrapper elasticSearchRestHighLevelClient() {
        final RestClientBuilder builder = RestClient.builder(new HttpHost(elasticsearchHost, elasticsearchPort, elasticsearchScheme));
        return new RestHighLevelClientWrapperImpl(new RestHighLevelClient(builder));
    }
}
