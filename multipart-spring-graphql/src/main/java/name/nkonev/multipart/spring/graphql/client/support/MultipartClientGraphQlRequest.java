package name.nkonev.multipart.spring.graphql.client.support;


import org.springframework.graphql.client.ClientGraphQlRequest;
import org.springframework.graphql.support.DefaultGraphQlRequest;
import org.springframework.lang.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultipartClientGraphQlRequest extends DefaultGraphQlRequest implements ClientGraphQlRequest {

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private final Map<String, Object> fileVariables = new ConcurrentHashMap<>();

    public MultipartClientGraphQlRequest(
        String document, @Nullable String operationName,
        Map<String, Object> variables, Map<String, Object> extensions,
        Map<String, Object> attributes,
        Map<String, Object> fileVariables) {

        super(document, operationName, variables, extensions);
        this.attributes.putAll(attributes);
        this.fileVariables.putAll(fileVariables);
    }

    public Map<String, Object> getFileVariables() {
        return this.fileVariables;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return this.attributes;
    }

}
