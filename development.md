# Making a release
```bash
export JAVA_HOME=/usr/lib/jvm/java-17
./mvnw clean
./mvnw -Dresume=false -DskipTests release:prepare release:perform
git fetch
```
