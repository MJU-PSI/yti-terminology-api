package fi.csc.termed.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan({ "fi.csc.termed.search" })
public class Application {

    @Autowired
    public ConfigurableApplicationContext context;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
