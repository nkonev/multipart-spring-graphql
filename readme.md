# Multipart Spring GraphQL
[![Maven Central](https://img.shields.io/maven-central/v/name.nkonev.multipart-spring-graphql/multipart-spring-graphql)](https://central.sonatype.com/namespace/name.nkonev.multipart-spring-graphql)

Adds [missing](https://github.com/spring-projects/spring-graphql/issues/69) multipart support.

Before using you need to see
* This [comment](https://github.com/spring-projects/spring-graphql/pull/430#issuecomment-1476186878)
* This [explanation](https://www.apollographql.com/blog/backend/file-uploads/file-upload-best-practices/)

## Features
1. `FilePart` arguments  for `Webflux` reactive [stack](https://github.com/nkonev/multipart-graphql-demo/tree/master/server-webflux)
```java
@Controller
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @MutationMapping(name = "fileUpload")
    public FileUploadResult uploadFile(@Argument FilePart file) {
        logger.info("Upload file: name={}", file.filename());

        return new FileUploadResult(UUID.randomUUID());
    }

    @MutationMapping(name = "multiFileUpload")
    public Collection<FileUploadResult> uploadMultiFiles(@Argument Collection<FilePart> files) {
        for (FilePart filePart : files) {
            logger.info("Upload file: name={}", filePart.filename());
        }
        return List.of(new FileUploadResult(UUID.randomUUID()));
    }

}
```

2. `MultipartFile` arguments for `WebMVC` servlet [stack](https://github.com/nkonev/multipart-graphql-demo/tree/master/server-webmvc)
```java
@Controller
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @MutationMapping(name = "fileUpload")
    public FileUploadResult uploadFile(@Argument MultipartFile file) {
        logger.info("Upload file: name={}", file.getOriginalFilename());

        return new FileUploadResult(UUID.randomUUID());
    }

    @MutationMapping(name = "multiFileUpload")
    public Collection<FileUploadResult> uploadMultiFiles(@Argument Collection<MultipartFile> files) {
        for (MultipartFile file : files) {
            logger.info("Upload file: name={}", file.getOriginalFilename());
        }
        return List.of(new FileUploadResult(UUID.randomUUID()));
    }

}
```

3. `WebClient`-based [client](https://github.com/nkonev/multipart-graphql-demo/tree/master/client-webflux)
```java
    @Autowired
    private MultipartGraphQlWebClient httpGraphQlClient;

    @Override
    public void run(String... args) {
        var doc = """
                mutation FileNUpload($files: [Upload!]) {
                    multiFileUpload(files: $files){
                        id
                    }
                }
                """;
        Map<String, Object> fileVariables = singletonMap("files", List.of(
            new ClassPathResource("/foo.txt"), 
            new ClassPathResource("/bar.txt")
        ));

        var request = MultipartClientGraphQlRequest.builder()
            .withDocument(doc)
            .withFileVariables(fileVariables)
            .build();
        var response = httpGraphQlClient.executeFileUpload("http://localhost:8899/graphql", request).block();
        LOGGER.info("Response is {}", response);
    }
```

4. `RestClient`-based [client](https://github.com/nkonev/multipart-graphql-demo/tree/master/client-webmvc)
```java
    @Autowired
    private MultipartGraphQlRestClient httpGraphQlClient;
    
    @Override
    public void run(String... args) {
        var doc = """
            mutation FileNUpload($files: [Upload!]) {
                multiFileUpload(files: $files){
                    id
                }
            }
            """;
        Map<String, Object> fileVariables = singletonMap("files", List.of(
            new ClassPathResource("/foo.txt"), 
            new ClassPathResource("/bar.txt")
        ));
        var request = MultipartClientGraphQlRequest.builder()
            .withDocument(doc)
            .withFileVariables(fileVariables)
            .build();
        var response = httpGraphQlClient.executeFileUpload("http://localhost:8889/graphql", request);
        LOGGER.info("Response is {}", response);
    } 
```

## Download

### Spring Boot Starter
```xml
<dependency>
  <groupId>name.nkonev.multipart-spring-graphql</groupId>
  <artifactId>multipart-spring-graphql</artifactId>
  <version>VERSION</version>
</dependency>
```

# Compatibility

| multipart-spring-graphql | Java | Spring Boot       | Example                                                      |
|--------------------------|------|-------------------|--------------------------------------------------------------|
| 0.10.x                   | 8+   | Spring Boot 2.7.x | https://github.com/nkonev/multipart-graphql-demo/tree/0.10.x |
| 1.0.x                    | 17+  | Spring Boot 3.0.x | https://github.com/nkonev/multipart-graphql-demo/tree/1.0.x  |
| 1.1.x                    | 17+  | Spring Boot 3.1.x | https://github.com/nkonev/multipart-graphql-demo/tree/1.1.x  |
| 1.2.x                    | 17+  | Spring Boot 3.2.x | https://github.com/nkonev/multipart-graphql-demo/tree/1.2.x  |
| 1.3.x                    | 17+  | Spring Boot 3.3.x | https://github.com/nkonev/multipart-graphql-demo/tree/1.3.x  |
| 1.4.x                    | 17+  | Spring Boot 3.3.x | https://github.com/nkonev/multipart-graphql-demo/tree/1.4.x  |
| 1.5.x                    | 17+  | Spring Boot 3.4.x | https://github.com/nkonev/multipart-graphql-demo/tree/1.5.x  |
