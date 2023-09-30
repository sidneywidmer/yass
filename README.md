# Yass

## DB Changes
- Add migration to resources
- Manually trigger flyaway:migrate
- Manually trigger jooq-codegen:generate
- flyway:migrate automatically gets triggered for unit tests, make sure th db url is correct in the env vars

## Error handling
We strongly differentiate between logical failures and exceptions. Anything that is out of the scope of our 
domain, or we can't handle it anyway because it's an _unexpected_ state, returns an exception. Anything that
we _can_ handle should be solved via arrow `raise`-DSL and context receivers.

## Config
Config file is under resources/application.conf. The environment variables need to be provided otherwise the application
won't start.