package name.nkonev.multipart.springboot.graphql.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.GraphQLScalarType;
import name.nkonev.multipart.spring.graphql.coercing.webmvc.UploadCoercing;
import name.nkonev.multipart.spring.graphql.server.webmvc.MultipartGraphQlHttpHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.graphql.autoconfigure.GraphQlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import static name.nkonev.multipart.spring.graphql.server.webmvc.MultipartGraphQlHttpHandler.SUPPORTED_MEDIA_TYPES;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@ConditionalOnProperty(value = "multipart.springboot.webmvc.server.enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@AutoConfiguration
public class MultipartGraphQlWebMvcAutoconfiguration {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurerUpload() {
        GraphQLScalarType uploadScalar = GraphQLScalarType.newScalar()
            .name("Upload")
            .coercing(new UploadCoercing())
            .build();

        return wiringBuilder -> wiringBuilder.scalar(uploadScalar);
    }

    @Bean
    @Order(-10)
    public RouterFunction<ServerResponse> graphQlMultipartRouterFunction(
        GraphQlProperties properties,
        WebGraphQlHandler webGraphQlHandler,
        ObjectMapper objectMapper
    ) {
        String path = properties.getHttp().getPath();
        RouterFunctions.Builder builder = RouterFunctions.route();
        MultipartGraphQlHttpHandler graphqlMultipartHandler = new MultipartGraphQlHttpHandler(webGraphQlHandler, new MappingJackson2HttpMessageConverter(objectMapper));
        builder = builder.POST(path, RequestPredicates.contentType(MULTIPART_FORM_DATA)
            .and(RequestPredicates.accept(SUPPORTED_MEDIA_TYPES.toArray(new MediaType[]{}))), graphqlMultipartHandler::handleMultipartRequest);
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    ObjectMapper jackson2ObjectMapper() {
        return new ObjectMapper();
    }
}
