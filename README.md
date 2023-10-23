# Yass

## DB Changes

- Add migration to resources
- Make sure to set db.migrate to true in the application.conf and start the application, this will automatically trigger the DB migrations (see Bootstrap.setupFlyway)
- Manually trigger jooq-codegen:generate

## Error handling

We strongly differentiate between logical failures and exceptions. Anything that is out of the scope of our
domain, or we can't handle it anyway because it's an _unexpected_ state, returns an exception. Anything that
we _can_ handle should be solved via arrow `raise`-DSL and context receivers. See DomainError.kt for examples
of errors we currently have.

## Config

Config file is under resources/application.conf. The environment variables need to be provided otherwise the application
won't start.

## Ideas / Todo's

Ordered by priority:
- [ ] Introduce TypeScript and Zustand to FE
- [ ] Build WS communication around centrifugal.dev
- [ ] Wiise -> Last rule-wise feature missing but booring... Idea: Not done automatically, user has to specifically wiise some cards
