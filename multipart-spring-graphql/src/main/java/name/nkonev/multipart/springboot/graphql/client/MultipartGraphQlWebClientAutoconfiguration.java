package name.nkonev.multipart.springboot.graphql.client;

import name.nkonev.multipart.spring.graphql.client.MultipartGraphQlWebClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@ConditionalOnClass(WebClient.class)
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
