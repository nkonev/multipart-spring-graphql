package name.nkonev.multipart.spring.graphql.server.webflux;

import graphql.schema.GraphQLScalarType;
import name.nkonev.multipart.spring.graphql.coercing.webflux.UploadCoercing;
import name.nkonev.multipart.spring.graphql.server.utils.GraphQlSetup;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static name.nkonev.multipart.spring.graphql.client.support.MultipartBodyCreator.createFilePartsAndMapping;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GraphQlHttpHandlerTests {

    private final JacksonJsonEncoder jackson2JsonEncoder = new JacksonJsonEncoder();

    @Test
    void shouldPassFile() {
        MultipartGraphQlHttpHandler handler = GraphQlSetup.schemaContent(
                """
                    type Query { ping: String }
                    scalar Upload
                    type Mutation {
                        fileUpload(fileInput: Upload!): String!
                    }
                """)
                .mutationFetcher("fileUpload", (env) -> ((FilePart) env.getVariables().get("fileInput")).filename())
                .runtimeWiring(builder -> builder.scalar(GraphQLScalarType.newScalar()
                        .name("Upload")
                        .coercing(new UploadCoercing())
                        .build()))
                .toHttpHandlerWebFluxMultipart();

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.ALL)
                .build();

        MockServerHttpResponse httpResponse = handleMultipartRequest(
                httpRequest, handler, """
                    mutation FileUpload($fileInput: Upload!)  {
                        fileUpload(fileInput: $fileInput)
                    }
                """,
                Collections.emptyMap(),
                Collections.singletonMap("fileInput", new ClassPathResource("/foo.txt"))
        );

        assertThat(httpResponse.getBodyAsString().block())
                .isEqualTo("{\"data\":{\"fileUpload\":\"foo.txt\"}}");
    }

    @Test
    void shouldPassListOfFiles() {
        MultipartGraphQlHttpHandler handler = GraphQlSetup.schemaContent(
                """
                    type Query { ping: String }
                    scalar Upload
                    type Mutation {
                        multipleFilesUpload(multipleFileInputs: [Upload!]!): [String!]!
                    }
                """)
                .mutationFetcher("multipleFilesUpload", (env) -> ((Collection<FilePart>) env.getVariables().get("multipleFileInputs")).stream().map(FilePart::filename).collect(Collectors.toList()))
                .runtimeWiring(builder -> builder.scalar(GraphQLScalarType.newScalar()
                        .name("Upload")
                        .coercing(new UploadCoercing())
                        .build()))
                .toHttpHandlerWebFluxMultipart();

        MockServerHttpRequest httpRequest = MockServerHttpRequest.post("/")
                .contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.ALL)
                .build();

        Collection<Resource> resources = new ArrayList<>();
        resources.add(new ClassPathResource("/foo.txt"));
        resources.add(new ClassPathResource("/bar.txt"));
        MockServerHttpResponse httpResponse = handleMultipartRequest(
                httpRequest, handler, """
                    mutation MultipleFilesUpload($multipleFileInputs: [Upload!]!) {
                        multipleFilesUpload(multipleFileInputs: $multipleFileInputs)
                    }
                """,
                Collections.emptyMap(),
                Collections.singletonMap("multipleFileInputs", resources)
        );

        assertThat(httpResponse.getBodyAsString().block())
                .isEqualTo("{\"data\":{\"multipleFilesUpload\":[\"foo.txt\",\"bar.txt\"]}}");
    }

    private MockServerHttpResponse handleMultipartRequest(
            MockServerHttpRequest httpRequest, MultipartGraphQlHttpHandler handler, String body,
            Map<String, Object> requestVariables, Map<String, Object> files) {

        MockServerWebExchange exchange = MockServerWebExchange.from(httpRequest);

        LinkedMultiValueMap<String, Part> parts = new LinkedMultiValueMap<>();

        Map<String, List<String>> partMappings = new HashMap<>();
        Map<String, Object> operations = new HashMap<>();
        operations.put("query", body);
        Map<String, Object> variables = new HashMap<>(requestVariables);
        createFilePartsAndMapping(files, variables, partMappings, (partName, resource) -> addFilePart(parts, partName, (Resource) resource));
        operations.put("variables", variables);
        addJsonEncodedPart(parts, "operations", operations);

        addJsonEncodedPart(parts, "map", partMappings);

        MockServerRequest serverRequest = MockServerRequest.builder()
                .exchange(exchange)
                .uri(((ServerWebExchange) exchange).getRequest().getURI())
                .method(((ServerWebExchange) exchange).getRequest().getMethod())
                .headers(((ServerWebExchange) exchange).getRequest().getHeaders())
                .body(Mono.just(parts));

        handler.handleMultipartRequest(serverRequest)
                .flatMap(response -> response.writeTo(exchange, new DefaultContext()))
                .block();

        return exchange.getResponse();
    }

    private void addJsonEncodedPart(LinkedMultiValueMap<String, Part> parts, String name, Object toSerialize) {
        ResolvableType resolvableType = ResolvableType.forClass(HashMap.class);
        Flux<DataBuffer> bufferFlux = jackson2JsonEncoder.encode(
                Mono.just(toSerialize),
                DefaultDataBufferFactory.sharedInstance,
                resolvableType,
                MediaType.APPLICATION_JSON,
                null
        );
        TestPart part = new TestPart(name, bufferFlux);
        parts.add(name, part);
    }

    private void addFilePart(LinkedMultiValueMap<String, Part> parts, String name, Resource resource) {
        Flux<DataBuffer> dataBufferFlux = DataBufferUtils.read(resource, DefaultDataBufferFactory.sharedInstance, 1024);
        TestFilePart filePart = new TestFilePart(name, resource.getFilename(), dataBufferFlux);
        parts.add(name, filePart);
    }

    private static class DefaultContext implements ServerResponse.Context {

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return Collections.singletonList(new EncoderHttpMessageWriter<>(new JacksonJsonEncoder()));
        }

        @Override
        public List<ViewResolver> viewResolvers() {
            return Collections.emptyList();
        }

    }

    private static class TestPart implements Part {

        private final String name;


        private final Flux<DataBuffer> content;

        private TestPart(String name,  Flux<DataBuffer> content) {
            this.name = name;
            this.content = content;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public HttpHeaders headers() {
            return new HttpHeaders();
        }

        @Override
        public Flux<DataBuffer> content() {
            return content;
        }
    }

    private static class TestFilePart implements FilePart {

        private final String name;

        private final String filename;

        private final Flux<DataBuffer> content;

        private TestFilePart(String name, String filename, Flux<DataBuffer> content) {
            this.name = name;
            this.filename = filename;
            this.content = content;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public HttpHeaders headers() {
            return new HttpHeaders();
        }

        @Override
        public Flux<DataBuffer> content() {
            return content;
        }

        @Override
        public String filename() {
            return filename;
        }

        @Override
        public Mono<Void> transferTo(Path dest) {
            return Mono.error(new RuntimeException("Not implemented"));
        }
    }

}
