package name.nkonev.multipart.spring.graphql.client.webmvc;

import name.nkonev.multipart.spring.graphql.client.support.MultipartClientGraphQlRequest;
import name.nkonev.multipart.spring.graphql.client.support.MultipartResponseMapGraphQlResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static name.nkonev.multipart.spring.graphql.client.support.MultipartBodyCreator.convertRequestToMultipartData;

public class MultipartGraphQlRestClient {

    private final RestClient restClient;

    private static final Log logger = LogFactory.getLog(MultipartGraphQlRestClient.class);

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
        new ParameterizedTypeReference<Map<String, Object>>() {
        };

    public MultipartGraphQlRestClient(RestClient restClient) {
        Assert.notNull(restClient, "WebClient is required");
        this.restClient = restClient;
    }

    public GraphQlResponse executeFileUpload(MultipartClientGraphQlRequest request) {
        return makeMultipartRequest(this.restClient.post(), request);
    }

    public GraphQlResponse executeFileUpload(String url, MultipartClientGraphQlRequest request) {
        return makeMultipartRequest(this.restClient.post().uri(url), request);
    }

    public GraphQlResponse executeFileUpload(String url, HttpHeaders headers, MultipartClientGraphQlRequest request) {
        return makeMultipartRequest(
            this.restClient.post()
                .uri(url)
                .headers(httpHeaders -> httpHeaders.addAll(headers)),
            request
        );
    }

    private MultipartResponseMapGraphQlResponse makeMultipartRequest(RestClient.RequestBodySpec spec, MultipartClientGraphQlRequest request) {
        var responseEntity = spec.contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON)
            .body(convertRequestToMultipartData(request))
            .retrieve()
            .toEntity(MAP_TYPE);
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            logger.debug("Got non 2xx status during sending graphql multipart request");
        }
        var map = responseEntity.getBody();
        return new MultipartResponseMapGraphQlResponse(map);
    }

}
