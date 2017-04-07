# Termed search

Termed is a web-based vocabulary and metadata editor.

Termed search handles updating of elastic search index.

## Install dependencies

Use Java 8u121 or later

Run with `./gradlew assemble` to download all dependencies.

## Running

Run with: `./gradlew bootRun`

Termed search should respond at port `8001`.

### Using profile-specific properties

To use different configurations based on Spring profile, such as *prod*, add a new property
file if it does not exist:
```
/src/main/resources/application-prod.properties
```

and run:
```
./gradlew bootRun -Dspring.profiles.active=prod
```

## Stopping

Run in a separate terminal: `curl -X POST localhost:8001/shutdown`

## Build (executable) war

Run with: `./gradlew build -Dspring.profiles.active=prod`