# Yass

## DB Changes
- Add migration to resources
- Manually trigger flyaway:migrate
- Manually trigger jooq-codegen:generate

## Error / Error-Codes

Types: 
- DomainError
  - OryError
  - RequestError
  - DbError

Codes: Error class indicates _where_ something when wrong - e.g Request, an API client, DB. 
The where references a system component. The specifies again where in the component something went wrong and 
ends with _what_ went wrong.

RequestError("header.token.missing") - where? Something with a request, header. what? token is missing