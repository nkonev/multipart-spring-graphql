package name.nkonev.multipart.spring.graphql.server.webflux;

import java.util.*;

import name.nkonev.multipart.spring.graphql.server.support.MultipartVariableMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.util.function.Tuple2;

import static org.springframework.http.MediaType.APPLICATION_GRAPHQL_RESPONSE;

public class MultipartGraphQlHttpHandler {
    private static final ParameterizedTypeReference<Map<String, List<String>>> LIST_PARAMETERIZED_TYPE_REF =
            new ParameterizedTypeReference<Map<String, List<String>>>() {};

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_PARAMETERIZED_TYPE_REF =
            new ParameterizedTypeReference<Map<String, Object>>() {};

    private final WebGraphQlHandler graphQlHandler;

    private final Decoder<?> jsonDecoder;

    public static final List<MediaType> SUPPORTED_MEDIA_TYPES =
            Arrays.asList(APPLICATION_GRAPHQL_RESPONSE, MediaType.APPLICATION_JSON, MediaType.APPLICATION_GRAPHQL);

    private static final Log logger = LogFactory.getLog(MultipartGraphQlHttpHandler.class);

    public MultipartGraphQlHttpHandler(WebGraphQlHandler graphQlHandler, Decoder<?> jsonDecoder) {
        Assert.notNull(graphQlHandler, "WebGraphQlHandler is required");
        Assert.notNull(jsonDecoder, "Decoder is required");
        this.graphQlHandler = graphQlHandler;
        this.jsonDecoder = jsonDecoder;
    }

    @SuppressWarnings("unchecked")
    public Mono<ServerResponse> handleMultipartRequest(ServerRequest serverRequest) {
        return serverRequest.multipartData()
                .flatMap(multipartMultiMap -> {
                    Map<String, Part> allParts = multipartMultiMap.toSingleValueMap();

                    Optional<Part> operation = Optional.ofNullable(allParts.get("operations"));
                    Optional<Part> mapParam = Optional.ofNullable(allParts.get("map"));

                    Decoder<Map<String, Object>> mapJsonDecoder = (Decoder<Map<String, Object>>) jsonDecoder;
                    Decoder<Map<String, List<String>>> listJsonDecoder = (Decoder<Map<String, List<String>>>) jsonDecoder;

                    Mono<Map<String, Object>> inputQueryMono = operation
                            .map(part -> mapJsonDecoder.decodeToMono(
                                    part.content(), ResolvableType.forType(MAP_PARAMETERIZED_TYPE_REF),
                                    MediaType.APPLICATION_JSON, null
                            )).orElse(Mono.just(new HashMap<>()));

                    Mono<Map<String, List<String>>> fileMapInputMono = mapParam
                            .map(part -> listJsonDecoder.decodeToMono(part.content(),
                                    ResolvableType.forType(LIST_PARAMETERIZED_TYPE_REF),
                                    MediaType.APPLICATION_JSON, null
                            )).orElse(Mono.just(new HashMap<>()));

                    return Mono.zip(inputQueryMono, fileMapInputMono)
                            .flatMap((Tuple2<Map<String, Object>, Map<String, List<String>>> objects) -> {
                                Map<String, Object> inputQuery = objects.getT1();
                                Map<String, List<String>> fileMapInput = objects.getT2();

                                final Map<String, Object> queryVariables = getFromMapOrEmpty(inputQuery, "variables");
                                final Map<String, Object> extensions = getFromMapOrEmpty(inputQuery, "extensions");

                                fileMapInput.forEach((String fileKey, List<String> objectPaths) -> {
                                    Part part = allParts.get(fileKey);
                                    if (part != null) {
                                        Assert.isInstanceOf(FilePart.class, part, "Part should be of type FilePart");
                                        FilePart file = (FilePart) part;
                                        objectPaths.forEach((String objectPath) -> {
                                            MultipartVariableMapper.mapVariable(
                                                    objectPath,
                                                    queryVariables,
                                                    file
                                            );
                                        });
                                    }
                                });

                                String query = (String) inputQuery.get("query");
                                String opName = (String) inputQuery.get("operationName");

                                Map<String, Object> body = new HashMap<>();
                                body.put("query", query);
                                body.put("operationName", StringUtils.hasText(opName) ? opName : "");
                                body.put("variables", queryVariables);
                                body.put("extensions", extensions);

                                WebGraphQlRequest graphQlRequest = new WebGraphQlRequest(
                                        serverRequest.uri(), serverRequest.headers().asHttpHeaders(),
                                        body,
                                        serverRequest.exchange().getRequest().getId(),
                                        serverRequest.exchange().getLocaleContext().getLocale());

                                if (logger.isDebugEnabled()) {
                                    logger.debug("Executing: " + graphQlRequest);
                                }
                                return this.graphQlHandler.handleRequest(graphQlRequest);
                            });
                })
                .flatMap(response -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Execution complete");
                    }
                    ServerResponse.BodyBuilder builder = ServerResponse.ok();
                    builder.headers(headers -> headers.putAll(response.getResponseHeaders()));
                    builder.contentType(selectResponseMediaType(serverRequest));
                    return builder.bodyValue(response.toMap());
                });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getFromMapOrEmpty(Map<String, Object> input, String key) {
        if (input.containsKey(key)) {
            return (Map<String, Object>)input.get(key);
        } else {
            return new HashMap<>();
        }
    }

    private static MediaType selectResponseMediaType(ServerRequest serverRequest) {
        for (MediaType accepted : serverRequest.headers().accept()) {
            if (SUPPORTED_MEDIA_TYPES.contains(accepted)) {
                return accepted;
            }
        }
        return MediaType.APPLICATION_JSON;
    }

}
