package fi.vm.yti.terminology.api.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AjpConfig {

    @Bean
    public ServletWebServerFactory servletContainer(@Value("${tomcat.ajp.port:}") Integer ajpPort) {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        if (ajpPort != null) {
            Connector ajpConnector = new Connector("AJP/1.3");
            ajpConnector.setPort(ajpPort);
            ajpConnector.setSecure(false);
            ajpConnector.setAllowTrace(false);
            ajpConnector.setScheme("http");
            factory.addAdditionalTomcatConnectors(ajpConnector);
        }
        return factory;
    }
}
