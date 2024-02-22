package name.nkonev.multipart.springboot;

import name.nkonev.multipart.spring.graphql.client.MultipartClientGraphQlRequest;
import name.nkonev.multipart.spring.graphql.client.MultipartGraphQlWebClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AutoconfigurationTest {

    private static final int PORT = 8891;

    @Controller
    public static class MyController {

        private final Logger logger = LoggerFactory.getLogger(MyController.class);

        @MutationMapping(name = "multiFileUpload")
        public Collection<FileUploadResult> uploadMultiFiles(@Argument Collection<MultipartFile> files) {
            var result = new ArrayList<FileUploadResult>();
            for (MultipartFile file : files) {
                logger.info("Upload file: name={}", file.getOriginalFilename());
                result.add(new FileUploadResult(file.getOriginalFilename()));
            }
            return result;
        }
    }

    @Configuration
    @EnableAutoConfiguration
    @AutoConfigureMockMvc
    public static class SimpleApp {

        @Autowired
        private MultipartGraphQlWebClient multipartGraphQlWebClient;

        private final Logger logger = LoggerFactory.getLogger(SimpleApp.class);

        public GraphQlResponse performAction() {
            var doc = """
                    mutation FileNUpload($files: [Upload!]) {
                        multiFileUpload(files: $files) {
                            id
                        }
                    }
                """;
            Map<String, Object> fileVariables = singletonMap(
                "files", List.of(new ClassPathResource("/foo.txt"), new ClassPathResource("/bar.txt"))
            );
            var request = new MultipartClientGraphQlRequest(
                doc,
                null,
                emptyMap(),
                emptyMap(),
                emptyMap(),
                fileVariables
            );
            var response = multipartGraphQlWebClient.executeFileUpload("http://localhost:"+PORT+"/graphql", request).block();
            logger.info("Response is {}", response);
            return response;
        }
    }

    @Test
    public void testWebMvc() {
        var builder = new SpringApplicationBuilder(SimpleApp.class, MyController.class);
        builder.properties("server.port=" + PORT);
        var ctx = builder.build().run();

        var bean = ctx.getBean(SimpleApp.class);
        var result = bean.performAction();

        Assertions.assertNotNull(result);
        var response = ((Map<String, List>)result.getData()).get("multiFileUpload");
        List<String> listOfFileNames = response.stream().map(o -> ((Map<String, String>)o).get("id")).toList();
        Assertions.assertEquals(2, listOfFileNames.size());
        Assertions.assertTrue(Set.of("foo.txt", "bar.txt").containsAll(listOfFileNames));
    }

    @Test
    public void testWithMockMvc() throws Exception {
        var builder = new SpringApplicationBuilder(SimpleApp.class, MyController.class);
        var ctx = builder.build().run();
        var mockMvc = ctx.getBean(MockMvc.class);

        var operations = """
            { "query" : "mutation FileNUpload($files: [Upload!]) { multiFileUpload(files: $files) { id } }",
              "variables": { "files": [null, null] } }
            """;

        var variables = """
            { "0": ["variables.files.0"], "1": ["variables.files.1"] }
            """;
        
        final var resource1 = new ClassPathResource("/foo.txt");
        final var resource2 = new ClassPathResource("/bar.txt");
        final var filePart1 = new MockMultipartFile("0", "foo.txt", "application/octet-stream", resource1.getContentAsByteArray());
        final var filePart2 = new MockMultipartFile("1", "bar.txt", "application/octet-stream", resource2.getContentAsByteArray());
        final var operationsPart = new MockPart("operations", operations.getBytes());
        final var variablesPart = new MockPart("map", variables.getBytes());

        final var asyncMvcResult = mockMvc.perform(MockMvcRequestBuilders
            .multipart("/graphql")
            .file(filePart1)
            .file(filePart2)
            .part(operationsPart)
            .part(variablesPart)
            .accept(MediaType.APPLICATION_GRAPHQL_RESPONSE_VALUE))
            .andReturn();

        mockMvc.perform(asyncDispatch(asyncMvcResult))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.multiFileUpload").isNotEmpty())
            .andExpect(jsonPath("$.data.multiFileUpload[0].id").value("foo.txt"))
            .andExpect(jsonPath("$.data.multiFileUpload[1].id").value("bar.txt"));

    }
}

class FileUploadResult {
    String id;

    public FileUploadResult(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
