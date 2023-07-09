package name.nkonev.multipart.spring.graphql.coercing.webmvc;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

public class UploadCoercing implements Coercing<MultipartFile, Object> {

    @Override
    public Object serialize(Object dataFetcherResult,
                            GraphQLContext graphQLContext,
                            Locale locale) throws CoercingSerializeException {
        throw new CoercingSerializeException("Upload is an input-only type");
    }

    @Override
    public MultipartFile parseValue(
        Object input,
        GraphQLContext graphQLContext,
        Locale locale
    ) throws CoercingParseValueException {
        if (input instanceof MultipartFile) {
            return (MultipartFile) input;
        }
        throw new CoercingParseValueException(
                String.format("Expected a 'MultipartFile' like object but was '%s'.",
                        input != null ? input.getClass() : null)
        );
    }

    @Override
    public MultipartFile parseLiteral(
        Value<?> input,
        CoercedVariables variables,
        GraphQLContext graphQLContext,
        Locale locale
    ) throws CoercingParseLiteralException {
        throw new CoercingParseLiteralException("Parsing literal of 'MultipartFile' is not supported");
    }
}
