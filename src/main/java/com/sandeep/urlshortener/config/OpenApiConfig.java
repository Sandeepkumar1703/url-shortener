package com.sandeep.urlshortener.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI urlShortenerOpenAPI() {

        return new OpenAPI()

                .info(new Info()
                        .title("URL Shortener API")
                        .description("Enterprise URL Shortener built with Spring Boot 3")
                        .version("1.0.0")

                        .contact(new Contact()
                                .name("Sandeep Kumar Prasad")
                                .email("your-sandeepkumarprasad01@example.com"))

                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))

                .externalDocs(new ExternalDocumentation()
                        .description("Project Repository")
                        .url("https://github.com/Sandeepkumar1703/url-shortener"));
    }

}