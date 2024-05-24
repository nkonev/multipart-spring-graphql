package name.nkonev.multipart.springboot.graphql.client;

import name.nkonev.multipart.spring.graphql.client.webmvc.MultipartGraphQlRestClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@AutoConfigureAfter(RestClientAutoConfiguration.class)
@ConditionalOnBean(RestClient.Builder.class)
@ConditionalOnProperty(value = "multipart.springboot.rest.client.enabled", matchIfMissing = true)
public class MultipartGraphQlRestClientAutoconfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public MultipartGraphQlRestClient multipartGraphQlRestClient(RestClient restClient) {
        return new MultipartGraphQlRestClient(restClient);
    }

}
