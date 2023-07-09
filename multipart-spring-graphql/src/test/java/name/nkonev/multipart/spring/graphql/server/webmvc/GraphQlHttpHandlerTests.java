package name.nkonev.multipart.spring.graphql.server.webmvc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.GraphQLScalarType;
import jakarta.servlet.ServletException;
import name.nkonev.multipart.spring.graphql.coercing.webmvc.UploadCoercing;
import name.nkonev.multipart.spring.graphql.server.utils.GraphQlSetup;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.function.AsyncServerResponse;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static name.nkonev.multipart.spring.graphql.client.MultipartBodyCreator.createFilePartsAndMapping;
import static org.assertj.core.api.Assertions.assertThat;

public class GraphQlHttpHandlerTests {

    private static final List<HttpMessageConverter<?>> MESSAGE_READERS =
            Collections.singletonList(new MappingJackson2HttpMessageConverter());

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPassFile() throws Exception {
        MultipartGraphQlHttpHandler handler = GraphQlSetup.schemaContent(
                """
                    type Query { ping: String }
                    scalar Upload
                    type Mutation {
                        fileUpload(fileInput: Upload!): String!
                    }
                """)
                .mutationFetcher("fileUpload", (env) -> ((MultipartFile) env.getVariables().get("fileInput")).getOriginalFilename())
                .runtimeWiring(builder -> builder.scalar(GraphQLScalarType.newScalar()
                        .name("Upload")
                        .coercing(new UploadCoercing())
                        .build()))
                .toHttpHandlerMultipart();
        MockHttpServletRequest servletRequest = createMultipartServletRequest(
                """
                    mutation FileUpload($fileInput: Upload!) {
                        fileUpload(fileInput: $fileInput)
                    }
                """,
                MediaType.APPLICATION_JSON_VALUE,
                Collections.singletonMap("fileInput", new ClassPathResource("/foo.txt"))
        );

        MockHttpServletResponse servletResponse = handleMultipartRequest(servletRequest, handler);

        assertThat(servletResponse.getContentAsString())
                .isEqualTo("{\"data\":{\"fileUpload\":\"foo.txt\"}}");
    }

    @Test
    void shouldPassListOfFiles() throws Exception {
        MultipartGraphQlHttpHandler handler = GraphQlSetup.schemaContent(
                """
                    type Query { ping: String }\s
                    scalar Upload
                    type Mutation {
                        multipleFilesUpload(multipleFileInputs: [Upload!]!): [String!]!
                    }
                """)
                .mutationFetcher("multipleFilesUpload", (env) -> ((Collection<MultipartFile>) env.getVariables().get("multipleFileInputs")).stream().map(multipartFile -> multipartFile.getOriginalFilename()).collect(Collectors.toList()))
                .runtimeWiring(builder -> builder.scalar(GraphQLScalarType.newScalar()
                        .name("Upload")
                        .coercing(new UploadCoercing())
                        .build()))
                .toHttpHandlerMultipart();

        Collection<Resource> resources = new ArrayList<>();
        resources.add(new ClassPathResource("/foo.txt"));
        resources.add(new ClassPathResource("/bar.txt"));

        MockHttpServletRequest servletRequest = createMultipartServletRequest(
            """
                    mutation MultipleFilesUpload($multipleFileInputs: [Upload!]!) {
                        multipleFilesUpload(multipleFileInputs: $multipleFileInputs)
                    }
                """,
                MediaType.APPLICATION_JSON_VALUE,
                Collections.singletonMap("multipleFileInputs", resources)
        );

        MockHttpServletResponse servletResponse = handleMultipartRequest(servletRequest, handler);

        assertThat(servletResponse.getContentAsString())
                .isEqualTo("{\"data\":{\"multipleFilesUpload\":[\"foo.txt\",\"bar.txt\"]}}");
    }

    private MockHttpServletRequest createMultipartServletRequest(String query, String accept, Map<String, Object> files) {
        MockMultipartHttpServletRequest servletRequest = new MockMultipartHttpServletRequest();
        servletRequest.addHeader("Accept", accept);
        servletRequest.setAsyncSupported(true);

        Map<String, List<String>> partMappings = new HashMap<>();
        Map<String, Object> operations = new HashMap<>();
        operations.put("query", query);
        Map<String, Object> variables = new HashMap<>();
        createFilePartsAndMapping(files, variables, partMappings,
                (partName, objectResource) -> servletRequest.addFile(getMultipartFile(partName, objectResource))
        );
        operations.put("variables", variables);

        servletRequest.addPart(new MockPart("operations", getJsonArray(operations)));
        servletRequest.addPart(new MockPart("map", getJsonArray(partMappings)));

        return servletRequest;
    }

    private MockMultipartFile getMultipartFile(String partName, Object objectResource) {
        Resource resource = (Resource) objectResource;
        try {
            return new MockMultipartFile(partName, resource.getFilename(), null, resource.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getJsonArray(Object o) {
        try {
            return objectMapper.writeValueAsBytes(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private MockHttpServletResponse handleMultipartRequest(
            MockHttpServletRequest servletRequest, MultipartGraphQlHttpHandler handler) throws ServletException, IOException {

        ServerRequest request = ServerRequest.create(servletRequest, MESSAGE_READERS);
        ServerResponse response = ((AsyncServerResponse) handler.handleMultipartRequest(request)).block();

        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        response.writeTo(servletRequest, servletResponse, new DefaultContext());
        return servletResponse;
    }

    private static class DefaultContext implements ServerResponse.Context {

        @Override
        public List<HttpMessageConverter<?>> messageConverters() {
            return MESSAGE_READERS;
        }

    }
}
