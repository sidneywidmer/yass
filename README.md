# Yass

## Start

Start the application for local development like so:
  1. `$ docker compse up`
  2. `$ ./mvnw exec:java -Dexec.mainClass="ch.yass.Yass"`

## DB Changes

- Add migration to resources
- Make sure to set db.migrate to true in the application.conf and start the application, this will automatically trigger
  the DB migrations (see Bootstrap.setupFlyway)
- Manually trigger jooq-codegen:generate

## Error handling

We strongly differentiate between logical failures and exceptions. Anything that is out of the scope of our
domain, or we can't handle it anyway because it's an _unexpected_ state, returns an exception. Anything that
we _can_ handle should be solved via arrow `raise`-DSL and context receivers. See DomainError.kt for examples
of errors we currently have.

## Config

Config file is under resources/application.conf. The environment variables need to be provided otherwise the application
won't start (direnv is a good solution for this `direnv`)

## Deployment

KISS: 
1. ./mvnw clean package -DskipTests
2. npm run build
3. rsync -avz --delete dist/ root@{server}:/var/www/frontend/
4. rsync -avz --delete target/server-1.0.0-SNAPSHOT.jar root@{server}:/opt/yass/yass.jar
5. ssh to server and `service yass restart`
