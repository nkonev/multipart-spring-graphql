package name.nkonev.multipart.spring.graphql.client;

import name.nkonev.multipart.spring.graphql.client.support.MultipartClientGraphQlRequest;
import name.nkonev.multipart.spring.graphql.client.utils.AbstractBuilderSetup;
import name.nkonev.multipart.spring.graphql.client.webflux.MultipartGraphQlWebClient;
import name.nkonev.multipart.spring.graphql.server.webflux.MultipartGraphQlHttpHandler;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.web.reactive.server.HttpHandlerConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import java.util.*;

import java.time.Duration;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

public class HttpGraphQlWebClientTests {

    private static final String DOCUMENT = "{ Mutation }";

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void shouldSendOneFile() {
        MultipartHttpBuilderSetup clientSetup = new MultipartHttpBuilderSetup();
        WebClient.Builder builder = clientSetup.initBuilder();
        WebClient webClient = builder.build();
        MultipartGraphQlWebClient client = new MultipartGraphQlWebClient(webClient);

        Map<String, Object> variables = singletonMap("existingVar", "itsValue");
        Map<String, Object> fileVariables = singletonMap("fileInput", new ClassPathResource("/foo.txt"));
        MultipartClientGraphQlRequest request = new MultipartClientGraphQlRequest(
            DOCUMENT,
            null,
            variables,
            emptyMap(),
            emptyMap(),
            fileVariables
        );
        client.executeFileUpload(request).block(TIMEOUT);

        assertThat(clientSetup.getActualRequest().getVariables().get("existingVar")).isEqualTo("itsValue");
        assertThat(clientSetup.getActualRequest().getVariables().get("fileInput")).isNotNull();
        assertThat(((FilePart)clientSetup.getActualRequest().getVariables().get("fileInput")).filename()).isEqualTo("foo.txt");
    }

    @Test
    void shouldSendOneCollectionOfFiles() {
        MultipartHttpBuilderSetup clientSetup = new MultipartHttpBuilderSetup();
        WebClient.Builder builder = clientSetup.initBuilder();
        WebClient webClient = builder.build();
        MultipartGraphQlWebClient client = new MultipartGraphQlWebClient(webClient);

        List<ClassPathResource> resources = new ArrayList<>();
        resources.add(new ClassPathResource("/foo.txt"));
        resources.add(new ClassPathResource("/bar.txt"));
        Map<String, Object> variables = singletonMap("existingVar", "itsValue");
        Map<String, Object> fileVariables = singletonMap("filesInput", resources);
        MultipartClientGraphQlRequest request = new MultipartClientGraphQlRequest(
                DOCUMENT,
                null,
                variables,
                emptyMap(),
                emptyMap(),
                fileVariables
        );
        client.executeFileUpload(request).block(TIMEOUT);

        assertThat(clientSetup.getActualRequest().getVariables().get("existingVar")).isEqualTo("itsValue");
        assertThat(clientSetup.getActualRequest().getVariables().get("filesInput")).isNotNull();
        assertThat(((Collection<FilePart>)clientSetup.getActualRequest().getVariables().get("filesInput")).size()).isEqualTo(2);
        assertThat(((Collection<FilePart>)clientSetup.getActualRequest().getVariables().get("filesInput")).stream().map(filePart -> filePart.filename()).collect(Collectors.toSet())).contains("foo.txt", "bar.txt");
    }

    private static class MultipartHttpBuilderSetup extends AbstractBuilderSetup {

        public WebClient.Builder initBuilder() {
            MultipartGraphQlHttpHandler handler = new MultipartGraphQlHttpHandler(webGraphQlHandler(), new JacksonJsonDecoder());
            RouterFunction<ServerResponse> routerFunction = route().POST("/**", handler::handleMultipartRequest).build();
            HttpHandler httpHandler = RouterFunctions.toHttpHandler(routerFunction, HandlerStrategies.withDefaults());
            HttpHandlerConnector connector = new HttpHandlerConnector(httpHandler);

            WebClient.Builder builder = WebClient.builder().clientConnector(connector);

            return builder;
        }

    }

}
