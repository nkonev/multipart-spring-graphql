package name.nkonev.multipart.spring.graphql.client.utils;

import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Extraction from org/springframework/graphql/client/WebGraphQlClientBuilderTests.java
 * @author Rossen Stoyanchev
*/
public abstract class AbstractBuilderSetup {

    @Nullable
    private WebGraphQlRequest graphQlRequest;

    private final MockExecutionGraphQlService graphQlService = new MockExecutionGraphQlService();

    public AbstractBuilderSetup() {
        this.graphQlService.setDefaultResponse("{}");
    }

    public MockExecutionGraphQlService getGraphQlService() {
        return this.graphQlService;
    }

    public WebGraphQlRequest getActualRequest() {
        Assert.state(this.graphQlRequest != null, "No saved WebGraphQlRequest");
        return this.graphQlRequest;
    }

    protected WebGraphQlHandler webGraphQlHandler() {
        return WebGraphQlHandler.builder(this.graphQlService)
                .interceptor((request, chain) -> {
                    this.graphQlRequest = request;
                    return chain.next(graphQlRequest);
                })
                .build();
    }

}
