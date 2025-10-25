package name.nkonev.multipart.spring.graphql.client.support;


import org.springframework.graphql.client.ClientGraphQlRequest;
import org.springframework.graphql.support.DefaultGraphQlRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String document;
        private String operationName;
        private Map<String, Object> variables = emptyMap();
        private Map<String, Object> extensions = emptyMap();
        private Map<String, Object> attributes = emptyMap();
        private Map<String, Object> fileVariables = emptyMap();

        public Builder withDocument(String document) {
            this.document = document;
            return this;
        }
        public Builder withOperationName(String operationName) {
            this.operationName = operationName;
            return this;
        }
        public Builder withVariables(Map<String, Object> variables) {
            this.variables = variables;
            return this;
        }
        public Builder withExtensions(Map<String, Object> extensions) {
            this.extensions = extensions;
            return this;
        }
        public Builder withAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }
        public Builder withMapFileVariables(Map<String, Object> fileVariables) {
            this.fileVariables = fileVariables;
            return this;
        }
        /**
         * Sets file variables
         * @param fileVariables vararg of something file-like. Example: withFileVariables(new ClassPathResource("/foo.txt"), new ClassPathResource("/bar.txt"))
         * @return the builder object
         */
        public Builder withFileVariables(Object... fileVariables) {
            this.fileVariables = singletonMap(MultipartGraphQlConstants.VARIABLE_NAME_FILES, Arrays.stream(fileVariables).toList());
            return this;
        }

        public MultipartClientGraphQlRequest build() {
            Assert.notNull(this.document, "document must not be null");
            return new MultipartClientGraphQlRequest(
                this.document,
                this.operationName,
                this.variables,
                this.extensions,
                this.attributes,
                this.fileVariables
            );
        }
    }
}
