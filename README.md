# Termed search

Termed is a web-based vocabulary and metadata editor.

Termed search handles updating of elastic search index.

## Install dependencies

Run with `./gradlew assemble` to download all dependencies.

## Running

Run with: `./gradlew bootRun`

Termed search should respond at port `8001`.

## Stopping

Run in a separate terminal: `curl -X POST localhost:8001/shutdown`

## Build (executable) war

Run with: `./gradlew build`

## Using profile-specific properties

To use different configurations based on Spring profile, such as *dev*, add a new property
file:
```
/src/main/resources/application-dev.properties
```

and run:
```
./gradlew bootRun -Dspring.profiles.active=dev
```

