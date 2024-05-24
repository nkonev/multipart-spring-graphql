package name.nkonev.multipart.springboot;

import name.nkonev.multipart.spring.graphql.client.support.MultipartClientGraphQlRequest;
import name.nkonev.multipart.spring.graphql.client.webflux.MultipartGraphQlWebClient;
import name.nkonev.multipart.spring.graphql.client.webmvc.MultipartGraphQlRestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

public class AutoconfigurationTest {

    private static final int PORT_WEBFLUX = 8891;
    private static final int PORT_WEBMVC = 8892;

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
    @EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
    public static class SimpleWebfluxApp {

        @Autowired
        private MultipartGraphQlWebClient multipartGraphQlWebClient;

        private final Logger logger = LoggerFactory.getLogger(SimpleWebfluxApp.class);

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
            var response = multipartGraphQlWebClient.executeFileUpload("http://localhost:"+ PORT_WEBFLUX +"/graphql", request).block();
            logger.info("Response is {}", response);
            return response;
        }
    }

    @Configuration
    @EnableAutoConfiguration(exclude = SecurityAutoConfiguration.class)
    public static class SimpleWebMvcApp {

        @Autowired
        private MultipartGraphQlRestClient multipartGraphQlWebClient;

        private final Logger logger = LoggerFactory.getLogger(SimpleWebMvcApp.class);

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
            var response = multipartGraphQlWebClient.executeFileUpload("http://localhost:"+ PORT_WEBMVC +"/graphql", request);
            logger.info("Response is {}", response);
            return response;
        }
    }

    @Test
    public void testWebflux() {
        var builder = new SpringApplicationBuilder(SimpleWebfluxApp.class, MyController.class);
        builder.properties("server.port=" + PORT_WEBFLUX);
        var ctx = builder.build().run();

        var bean = ctx.getBean(SimpleWebfluxApp.class);
        var result = bean.performAction();

        Assertions.assertNotNull(result);
        var response = ((Map<String, List>)result.getData()).get("multiFileUpload");
        List<String> listOfFileNames = response.stream().map(o -> ((Map<String, String>)o).get("id")).toList();
        Assertions.assertEquals(2, listOfFileNames.size());
        Assertions.assertTrue(Set.of("foo.txt", "bar.txt").containsAll(listOfFileNames));
    }

    @Test
    public void testWebMvc() {
        var builder = new SpringApplicationBuilder(SimpleWebMvcApp.class, MyController.class);
        builder.properties("server.port=" + PORT_WEBMVC);
        var ctx = builder.build().run();

        var bean = ctx.getBean(SimpleWebMvcApp.class);
        var result = bean.performAction();

        Assertions.assertNotNull(result);
        var response = ((Map<String, List>)result.getData()).get("multiFileUpload");
        List<String> listOfFileNames = response.stream().map(o -> ((Map<String, String>)o).get("id")).toList();
        Assertions.assertEquals(2, listOfFileNames.size());
        Assertions.assertTrue(Set.of("foo.txt", "bar.txt").containsAll(listOfFileNames));
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
