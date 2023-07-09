# Multipart Spring GraphQL

Adds [missing](https://github.com/spring-projects/spring-graphql/issues/69) multipart support.

Before using you need to see
* This [comment](https://github.com/spring-projects/spring-graphql/pull/430#issuecomment-1476186878)
* This [explanation](https://www.apollographql.com/blog/backend/file-uploads/file-upload-best-practices/)

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

| multipart-spring-graphql | Java | Spring Boot        | Example                                                         |
|--------------------------|------|--------------------|-----------------------------------------------------------------|
| 0.10.x                   | 8+   | Spring Boot 2.7.x  | https://github.com/nkonev/multipart-graphql-demo/tree/0.10.x    |
| ------------------------ | -----| ------------------ | --------------------------------------------------------------- |
| 1.0.x                    | 17+  | Spring Boot 3.0.x  | https://github.com/nkonev/multipart-graphql-demo/tree/1.0.x     |
