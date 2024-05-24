package name.nkonev.multipart.springboot.graphql.client;

import name.nkonev.multipart.spring.graphql.client.webflux.MultipartGraphQlWebClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@AutoConfigureAfter(WebClientAutoConfiguration.class)
@ConditionalOnClass(WebClient.class)
@ConditionalOnBean(WebClient.Builder.class)
@ConditionalOnProperty(value = "multipart.springboot.webflux.client.enabled", matchIfMissing = true)
public class MultipartGraphQlWebClientAutoconfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public MultipartGraphQlWebClient multipartGraphQlWebClient(WebClient webClient) {
        return new MultipartGraphQlWebClient(webClient);
    }

}
