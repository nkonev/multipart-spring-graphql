package name.nkonev.multipart.spring.graphql.coercing.webflux;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import org.springframework.http.codec.multipart.FilePart;

import java.util.Locale;

public class UploadCoercing implements Coercing<FilePart, Object> {

    @Override
    public Object serialize(
        Object dataFetcherResult,
        GraphQLContext graphQLContext,
        Locale locale
    ) throws CoercingSerializeException {
        throw new CoercingSerializeException("Upload is an input-only type");
    }

    @Override
    public FilePart parseValue(
        Object input,
        GraphQLContext graphQLContext,
        Locale locale
    ) throws CoercingParseValueException {
        if (input instanceof FilePart) {
            return (FilePart) input;
        }
        throw new CoercingParseValueException(
            String.format("Expected 'FilePart' like object but was '%s'.",
                input != null ? input.getClass() : null)
        );
    }

    @Override
    public FilePart parseLiteral(
        Value<?> input,
        CoercedVariables variables,
        GraphQLContext graphQLContext,
        Locale locale
    ) throws CoercingParseLiteralException {
        throw new CoercingParseLiteralException("Parsing literal of 'FilePart' is not supported");
    }
}

