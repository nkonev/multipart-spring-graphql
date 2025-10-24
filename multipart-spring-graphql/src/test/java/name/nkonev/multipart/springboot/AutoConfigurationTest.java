package name.nkonev.multipart.springboot;

import name.nkonev.multipart.spring.graphql.client.support.MultipartClientGraphQlRequest;
import name.nkonev.multipart.spring.graphql.client.webflux.MultipartGraphQlWebClient;
import name.nkonev.multipart.spring.graphql.client.webmvc.MultipartGraphQlRestClient;
import name.nkonev.multipart.springboot.graphql.client.MultipartGraphQlRestClientAutoconfiguration;
import name.nkonev.multipart.springboot.graphql.client.MultipartGraphQlWebClientAutoconfiguration;
import name.nkonev.multipart.springboot.graphql.server.MultipartGraphQlWebFluxAutoconfiguration;
import name.nkonev.multipart.springboot.graphql.server.MultipartGraphQlWebMvcAutoconfiguration;
import name.nkonev.multipart.springboot.graphql.server.SchemaAutoconfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.graphql.autoconfigure.GraphQlAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.reactive.GraphQlWebFluxAutoConfiguration;
import org.springframework.boot.graphql.autoconfigure.servlet.GraphQlWebMvcAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.reactor.autoconfigure.ReactorAutoConfiguration;
import org.springframework.boot.reactor.netty.autoconfigure.NettyReactiveWebServerAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.MultipartAutoConfiguration;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.HttpHandlerAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.ReactiveMultipartAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

public class AutoConfigurationTest {

    private static final int PORT_WEBFLUX = 8891;
    private static final int PORT_WEBMVC = 8892;

    @Controller
    public static class MyControllerWebFlux {

        private final Logger logger = LoggerFactory.getLogger(MyControllerWebFlux.class);

        @MutationMapping(name = "multiFileUpload")
        public Collection<FileUploadResult> uploadMultiFiles(@Argument Collection<FilePart> files) {
            var result = new ArrayList<FileUploadResult>();
            for (FilePart file: files) {
                logger.info("Upload file: name={}", file.filename());
                result.add(new FileUploadResult(file.filename()));
            }
            return result;
        }
    }

    @Controller
    public static class MyControllerWebMvc {

        private final Logger logger = LoggerFactory.getLogger(MyControllerWebMvc.class);

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

    @ImportAutoConfiguration(classes = {
            // common
            GraphQlAutoConfiguration.class,

            // server
            NettyReactiveWebServerAutoConfiguration.class,
            ReactorAutoConfiguration.class,
            WebFluxAutoConfiguration.class,
            HttpHandlerAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            ReactiveMultipartAutoConfiguration.class,
            GraphQlWebFluxAutoConfiguration.class,
            MultipartGraphQlWebFluxAutoconfiguration.class,
            SchemaAutoconfiguration.class,

            // client
            WebClientAutoConfiguration.class,
            MultipartGraphQlWebClientAutoconfiguration.class,
    })
    @Configuration
    public static class WebFluxConfiguration {

    }

    @Service
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

    @ImportAutoConfiguration(classes = {
            TomcatServletWebServerAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class,
            WebMvcAutoConfiguration.class,
            JacksonAutoConfiguration.class,
            RestClientAutoConfiguration.class,
            MultipartAutoConfiguration.class,
            GraphQlAutoConfiguration.class,
            GraphQlWebMvcAutoConfiguration.class,
            MultipartGraphQlRestClientAutoconfiguration.class,
            MultipartGraphQlWebMvcAutoconfiguration.class,
            SchemaAutoconfiguration.class,
    })
    @Configuration
    public static class WebMvcConfiguration {

    }

    @Service
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
    public void testWebFlux() {
        var builder = new SpringApplicationBuilder(WebFluxConfiguration.class, SimpleWebfluxApp.class, MyControllerWebFlux.class);
        builder.properties("server.port=" + PORT_WEBFLUX);
        builder.properties("logging.level.org.springframework.web=TRACE");
        builder.web(WebApplicationType.REACTIVE);
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
        var builder = new SpringApplicationBuilder(WebMvcConfiguration.class, SimpleWebMvcApp.class, MyControllerWebMvc.class);
        builder.properties("server.port=" + PORT_WEBMVC);
        builder.properties("logging.level.org.springframework.web=TRACE");
        builder.web(WebApplicationType.SERVLET);
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
