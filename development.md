# Making a release
```bash
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk
./mvnw clean
./mvnw -Dresume=false -DskipTests release:prepare release:perform
git fetch
```
