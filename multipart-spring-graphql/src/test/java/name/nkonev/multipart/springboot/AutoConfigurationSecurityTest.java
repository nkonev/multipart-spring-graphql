package name.nkonev.multipart.springboot;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AutoConfigurationSecurityTest {

    @AutoConfigureMockMvc
    @SpringBootConfiguration
    @EnableAutoConfiguration
    public static class MyConfig {

    }

    @Configuration
    @EnableWebSecurity
    public static class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .authorizeHttpRequests((authorizeRequests) ->
                    authorizeRequests
                        .requestMatchers("/**").hasRole("USER")
                )
                .httpBasic(withDefaults());
            return http.build();
        }

        @Bean
        public UserDetailsService userDetailsService() {
            UserDetails user = User.withDefaultPasswordEncoder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();
            return new InMemoryUserDetailsManager(user);
        }
    }

    @Controller
    public static class MyController2 {

        private final Logger logger = LoggerFactory.getLogger(MyController2.class);

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

    @Test
    public void testWithMockMvc() throws Exception {
        var builder = new SpringApplicationBuilder(MyConfig.class, SecurityConfig.class, MyController2.class)
            .web(WebApplicationType.SERVLET);
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



        mockMvc.perform(
            MockMvcRequestBuilders
                .multipart("/graphql")
                .file(filePart1)
                .file(filePart2)
                .part(operationsPart)
                .part(variablesPart)
                .accept(MediaType.APPLICATION_GRAPHQL_RESPONSE_VALUE)
                .with(csrf())
                .with(SecurityMockMvcRequestPostProcessors.httpBasic("user", "password"))
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.multiFileUpload").isNotEmpty())
            .andExpect(jsonPath("$.data.multiFileUpload[0].id").value("foo.txt"))
            .andExpect(jsonPath("$.data.multiFileUpload[1].id").value("bar.txt"));

    }
}

