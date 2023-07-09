# Making a release
```bash
./mvnw clean
./mvnw -Dresume=false -DskipTests release:prepare release:perform
git fetch
```
