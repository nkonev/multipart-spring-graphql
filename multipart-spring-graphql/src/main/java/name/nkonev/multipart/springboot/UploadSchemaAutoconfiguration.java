package name.nkonev.multipart.springboot;

import org.springframework.boot.autoconfigure.AbstractDependsOnBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.graphql.GraphQlAutoConfiguration;
import org.springframework.boot.autoconfigure.graphql.GraphQlSourceBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.graphql.execution.GraphQlSource;

@ConditionalOnProperty(value = "multipart.springboot.inject-upload-scalar.enabled", matchIfMissing = true)
@AutoConfiguration
@AutoConfigureBefore(GraphQlAutoConfiguration.class)
public class UploadSchemaAutoconfiguration {
    @Bean
    public AddUploadScalarGraphQlSourceBuilderCustomizer addUploadScalarGraphQlSourceBuilderCustomizer() {
        return new AddUploadScalarGraphQlSourceBuilderCustomizer();
    }

    // declares that GraphQlSource depends on AddUploadScalarGraphQlSourceBuilderCustomizer
    @Configuration(proxyBeanMethods = false)
    public static class GraphQlSourceDependsOnAddUploadScalarGraphQlSourceBuilderCustomizerBeanFactoryPostProcessor extends AbstractDependsOnBeanFactoryPostProcessor {

        protected GraphQlSourceDependsOnAddUploadScalarGraphQlSourceBuilderCustomizerBeanFactoryPostProcessor() {
            super(GraphQlSource.class, AddUploadScalarGraphQlSourceBuilderCustomizer.class);
        }
    }

}

class AddUploadScalarGraphQlSourceBuilderCustomizer implements GraphQlSourceBuilderCustomizer {

    @Override
    public void customize(GraphQlSource.SchemaResourceBuilder builder) {
        builder.schemaResources(new ClassPathResource("multipart-spring-graphql/upload.graphqls"));
    }
}
