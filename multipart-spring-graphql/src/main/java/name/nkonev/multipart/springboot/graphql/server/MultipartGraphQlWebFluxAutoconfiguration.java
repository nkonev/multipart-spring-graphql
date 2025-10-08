package name.nkonev.multipart.springboot.graphql.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.GraphQLScalarType;
import name.nkonev.multipart.spring.graphql.coercing.webflux.UploadCoercing;
import name.nkonev.multipart.spring.graphql.server.webflux.MultipartGraphQlHttpHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.graphql.autoconfigure.GraphQlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static name.nkonev.multipart.spring.graphql.server.webflux.MultipartGraphQlHttpHandler.SUPPORTED_MEDIA_TYPES;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

@ConditionalOnProperty(value = "multipart.springboot.webflux.server.enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@AutoConfiguration
public class MultipartGraphQlWebFluxAutoconfiguration {

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
        MultipartGraphQlHttpHandler graphqlMultipartHandler = new MultipartGraphQlHttpHandler(webGraphQlHandler, new Jackson2JsonDecoder(objectMapper));
        builder = builder.POST(path, RequestPredicates.contentType(MULTIPART_FORM_DATA)
            .and(RequestPredicates.accept(SUPPORTED_MEDIA_TYPES.toArray(new MediaType[]{}))), graphqlMultipartHandler::handleMultipartRequest);
        return builder.build();
    }

}
