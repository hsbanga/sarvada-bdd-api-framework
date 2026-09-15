# Sarvada BDD API Automation Framework

Behaviour-driven REST API test automation by **Sarvada Infotech**, built on
**Java 21 · Cucumber 7 · Rest Assured 6 · TestNG · Maven**.

The framework provides a reusable, data-driven Gherkin vocabulary: feature files describe requests
and expectations in plain English, while base URLs, endpoint paths and test data live in
configuration. Onboarding a new API means adding endpoints and data, not writing Java.

The repository ships with a runnable demo suite against the public
[JSONPlaceholder](https://jsonplaceholder.typicode.com) service covering CRUD flows, filtering,
schema validation, chained requests and negative cases.

## Quick start

Requirements: JDK 21. Maven is bundled through the wrapper.

```bash
./mvnw verify                                          # whole suite, dev environment
./mvnw verify -Dcucumber.filter.tags="@smoke"          # only smoke scenarios
./mvnw verify -Dthreads=4                              # 4 scenarios in parallel
./mvnw verify -Denv=stag                               # use config/stag.properties
./mvnw test -Dcucumber.features=@target/cucumber-reports/rerun.txt   # re-run only the failures
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

Reports after a run:

| Artifact | Location |
|---|---|
| Rich HTML report (Masterthought) | `target/cucumber-html-report/cucumber-html-reports/overview-features.html` |
| Cucumber HTML / JSON / JUnit | `target/cucumber-reports/` |
| Parallel timeline | `target/cucumber-reports/timeline/index.html` |
| Execution log with every request and response | `target/logs/test-run.log` |

## Writing scenarios

```gherkin
Scenario: Create a post, then read the author it belongs to
  Given I use the "default" API
  And I set the request body to:
    | userId | ${data:USER_ID}              |
    | title  | Sarvada BDD ${random:string} |
  When I send a POST request to "POSTS"
  Then the response status code should be 201
  And the response should match the schema "schemas/post.schema.json"
  And I save the response field "userId" as "authorId"
  Given I add "id" as "${saved:authorId}" in "PATH"
  When I send a GET request to "USER_BY_ID"
  Then the response status code should be 200
  And the "POSTS" response field "userId" should equal the "USER_BY_ID" response field "id"
```

### Request vocabulary

| Step | Meaning |
|---|---|
| `Given I use the "<alias>" API` | selects `api.<alias>.base.url` from configuration (default: `default`) |
| `Given I add "<CONDITION>" "<DATA_NAME>" in "<PLACE>"` | adds a test-data item to QUERY, HEADER, PATH, FORM or COOKIE |
| `Given I add "<CONDITION>" "<DATA_NAME>" in "<PLACE>" as "<param>"` | same, when the parameter name differs from the data name |
| `Given I add "<name>" as "<value>" in "<PLACE>"` | adds a literal value; placeholders allowed |
| `Given I add the following in "<PLACE>":` + table | several parameters at once |
| `Given I set the request body:` + doc string | raw JSON body; placeholders allowed |
| `Given I set the request body to:` + table | flat JSON object, numbers and booleans typed automatically |
| `Given I set the request body from file "<classpath file>"` | body from `src/test/resources` |
| `When I send a <METHOD> request to "<ENDPOINT or /path>"` | GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS |

**Conditions** turn one piece of test data into positive and negative variants without extra files:

| Condition | Value used |
|---|---|
| `CORRECT` | `data.<NAME>` from the environment file, or a value saved earlier in the scenario |
| `INCORRECT` | the correct value with its last character changed (same shape, wrong value) |
| `INVALID` | a random string of the wrong shape |
| `NULL` | empty string |
| `EXPIRED` | `data.EXPIRED_<NAME>` |

**Placeholders** work inside any literal: `${data:NAME}`, `${saved:key}`, `${random:email|uuid|int|name|string}`.

### Response vocabulary

| Step | Checks |
|---|---|
| `the response status code should be <int>` | HTTP status (body printed on failure) |
| `the response time should be under <int> ms` | latency budget |
| `the response header "<name>" should contain "<text>"` | header value |
| `the response field "<jsonPath>" should be / should contain "<value>"` | JSON field, placeholders allowed |
| `the response field "<jsonPath>" should exist / should not exist` | presence |
| `the response field "<jsonPath>" should be a valid URL` | URL format |
| `the response body should be empty` | `""`, `{}` or `[]` |
| `the response should be a list of <int> items` | root array size |
| `the response field "<jsonPath>" should be a list of <int> items / a non-empty list` | nested array size |
| `every item in "<jsonPath>" should have "<field>" equal to "<value>"` | filter results |
| `the response should match the schema "<classpath json schema>"` | contract test |
| `I save the response field "<jsonPath>" as "<key>"` | capture for later steps |
| `the response field "<jsonPath>" should equal the saved "<key>"` | compare with captured value |
| `the "<ENDPOINT A>" response field "<path>" should equal the "<ENDPOINT B>" response field "<path>"` | cross-response consistency |

## Configuration

| File | Purpose |
|---|---|
| `endpoints.properties` | `NAME=/path/{param}` registry used by feature files |
| `config/global.properties` | timeouts, retry policy, logging |
| `config/{dev,stag,prod}.properties` | `api.<alias>.base.url` and `data.<NAME>` test data per environment |
| `config/local.properties` | git-ignored developer overrides |
| `schemas/*.json` | JSON Schemas for contract checks |

Resolution order for every key: `-Dkey` flag > environment variable (`API_DEFAULT_BASE_URL`,
`DATA_API_KEY`, ...) > `local.properties` > `<env>.properties` > `global.properties`.
Secrets are never committed: export them or store them as CI secrets.

Credential-bearing headers (`Authorization`, `X-Api-Key`, tokens, cookies) are masked in the log.

## Project layout

```
src/test
├── java/com/sarvadainfotech/bdd/api
│   ├── runner/TestRunner.java         TestNG + Cucumber entry point, parallel data provider
│   ├── hooks/Hooks.java               scenario logging, last response attached on failure
│   ├── steps/RequestSteps.java        generic request-building vocabulary
│   ├── steps/ResponseSteps.java       generic assertion vocabulary
│   ├── core/ApiClient.java            builds and sends requests, logs traffic, retries on 429
│   ├── core/Endpoints.java            endpoint registry
│   ├── core/ValueResolver.java        CORRECT / INCORRECT / ... conditions and ${...} placeholders
│   ├── context/RequestContext.java    per-scenario request, responses and saved values (PicoContainer)
│   ├── config/Config.java             layered configuration
│   └── util/RandomData.java           unique test data
└── resources
    ├── features/*.feature
    ├── endpoints.properties
    ├── config/*.properties
    ├── schemas/*.json
    ├── testng.xml
    └── log4j2.xml
```

## Design principles

- **API-agnostic steps.** No step definition knows a URL, a field name or a credential. Adding a
  new service is configuration plus feature files.
- **Isolated, parallel-safe scenarios.** All state lives in a per-scenario `RequestContext`
  injected by PicoContainer; there are no static mutable fields. `-Dthreads=N` just works.
- **Negative testing for free.** Conditions derive wrong, invalid, missing and expired variants
  from a single correct value.
- **Chained calls.** Save any response field and reuse it in later requests or assertions,
  including cross-response comparisons between two endpoints.
- **Contract checks.** JSON Schema validation is one step away.
- **Observability.** Every request and response is written to the log with secrets masked, and the
  last response is attached to the report when a scenario fails.

## Continuous integration

`.github/workflows/api-tests.yml` runs the regression suite on every push and pull request, can be
triggered manually with a tag expression and environment, and uploads the reports as build
artifacts. API keys are injected from repository secrets.

## License

Copyright Sarvada Infotech. All rights reserved.
