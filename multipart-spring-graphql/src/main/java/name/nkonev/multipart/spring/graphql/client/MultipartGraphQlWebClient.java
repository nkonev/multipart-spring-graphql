package name.nkonev.multipart.spring.graphql.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static name.nkonev.multipart.spring.graphql.client.MultipartBodyCreator.convertRequestToMultipartData;

public class MultipartGraphQlWebClient {

    private final WebClient webClient;

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<Map<String, Object>>() {};

    public MultipartGraphQlWebClient(WebClient webClient) {
        Assert.notNull(webClient, "WebClient is required");
        this.webClient = webClient;
    }

    public Mono<GraphQlResponse> executeFileUpload(MultipartClientGraphQlRequest request) {
        return this.webClient.post()
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_GRAPHQL)
                .body(BodyInserters.fromMultipartData(convertRequestToMultipartData(request)))
                .retrieve()
                .bodyToMono(MAP_TYPE)
                .map(ResponseMapGraphQlResponse::new);
    }
}
