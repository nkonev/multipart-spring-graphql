package name.nkonev.multipart.spring.graphql.client.support;

import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.function.BiConsumer;

public final class MultipartBodyCreator {

    public static MultiValueMap<String, ?> convertRequestToMultipartData(MultipartClientGraphQlRequest multipartRequest) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        Map<String, List<String>> partMappings = new HashMap<>();
        Map<String, Object> operations = multipartRequest.toMap();
        Map<String, Object> variables = new HashMap<>(multipartRequest.getVariables());
        createFilePartsAndMapping(multipartRequest.getFileVariables(), variables, partMappings, builder::part);
        operations.put(MultipartGraphQlConstants.VARIABLES, variables);
        Map<String, Object> extensions = new HashMap<>(multipartRequest.getExtensions());
        operations.put(MultipartGraphQlConstants.EXTENSIONS, extensions);
        builder.part(MultipartGraphQlConstants.OPERATIONS, operations);

        builder.part(MultipartGraphQlConstants.MAP, partMappings);
        return builder.build();
    }

    public static void createFilePartsAndMapping(
        Map<String, ?> fileVariables,
        Map<String, Object> variables,
        Map<String, List<String>> partMappings,
        BiConsumer<String, Object> partConsumer
    ) {
        final String uploadPartPrefix = "uploadPart";

        int partNumber = 0;
        for (Map.Entry<String, ?> entry : fileVariables.entrySet()) {
            Object resource = entry.getValue();
            String variableName = entry.getKey();
            if (resource instanceof Collection) {
                List<Object> placeholders = new ArrayList<>();
                int inMappingNumber = 0;
                for (Object fileResourceItem : (Collection) resource) {
                    placeholders.add(null);
                    String partName = uploadPartPrefix + partNumber;
                    partConsumer.accept(partName, fileResourceItem);
                    partMappings.put(partName, Collections.singletonList(
                        MultipartGraphQlConstants.VARIABLES+"." + variableName + "." + inMappingNumber
                    ));
                    partNumber++;
                    inMappingNumber++;
                }
                variables.put(variableName, placeholders);
            } else {
                String partName = uploadPartPrefix + partNumber;
                partConsumer.accept(partName, resource);
                variables.put(variableName, null);
                partMappings.put(partName, Collections.singletonList(MultipartGraphQlConstants.VARIABLES+"." + variableName));
                partNumber++;
            }
        }
    }

}
