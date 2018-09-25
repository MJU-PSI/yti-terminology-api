package fi.vm.yti.terminology.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    @Bean
    public Docket frontendApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("frontend")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.ant("/frontend/**"))
                .build();
    }

    @Bean
    public Docket reindexApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("reindex")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.ant("/reindex"))
                .build();
    }

    @Bean
    public Docket synchronizeApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("synchronize")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.ant("/synchronize"))
                .build();
    }

    @Bean
    public Docket importApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("import")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.ant("/frontend/import/**"))
                .build();
    }

    @Bean
    public Docket integrationApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .groupName("integration")
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.ant("/integration/**"))
                .build();
    }

}
