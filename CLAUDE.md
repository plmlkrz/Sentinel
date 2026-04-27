# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sentinel is a Selenium WebDriver automation framework implementing the Page Object Model (POM). It abstracts Selenium complexity so tests can be written in BDD-style Gherkin (Cucumber). It is published to Maven Central and consumed by downstream test projects — users write tests against Sentinel, not within it.

- **Supported automation types:** Web UI (Chrome/Firefox/Edge/Safari/IE), Windows desktop (WinAppDriver/Appium), REST APIs, PDF/CSV/image verification
- **Maven coordinates:** `io.github.sentinel:sentinel:1.0.13-SNAPSHOT`
- **Java 17**, TestNG + Cucumber 7.8.1

## Build & Test Commands

```bash
# Build
mvn install

# Run all tests
mvn test

# Run tests by Cucumber tag
mvn test -Dcucumber.filter.tags="@YourTag"

# Run a specific unit test class
mvn test -Dtest=ElementTests

# Run a specific test method
mvn test -Dtest=ElementTests#testMethodName

# Generate code coverage report (outputs to target/site/jacoco/index.html)
mvn clean test jacoco:report

# Generate Javadocs (outputs to docs/)
mvn javadoc:javadoc
```

Tests are driven by `testng.xml` at the project root. The main test entry points are:
- `src/test/java/tests/TestNGSentinelTests.java` (TestNG)
- `src/test/java/tests/SentinelTests.java` (JUnit Platform Suite)

Feature files live in `src/test/java/features/`. Test reports output to `reports/`.

## Architecture

### Core Design

The framework uses:
- **Page Object Model via YAML:** Pages are defined as YAML files with element selectors. `PageFactory` loads them; `PageManager` tracks the current page. Elements are resolved lazily (at interaction time, not page load).
- **Factory pattern:** `ElementFactory`, `WebDriverFactory`, `PageFactory` create objects.
- **Singleton:** `Driver` (single WebDriver instance per page object type) and `Configuration` (loaded from `conf/sentinel.yml`).
- **BDD Glue Code:** Cucumber step definitions in `io.github.sentinel.steps` translate natural-language steps into element interactions.

### Package Responsibilities

| Package | Role |
|---|---|
| `configurations` | Loads `conf/sentinel.yml`; provides env-specific settings (browser, URLs, credentials, timeouts) |
| `pages` | `Page` base class, `PageManager` (current page tracker), `PageData`, `PageFactory` |
| `elements` | `Element` base class + `Textbox`, `WindowsElement`; `ElementFactory` for instantiation |
| `elements.dropdowns` | Dropdown implementations: `SelectElement`, `MaterialUISelect`, `PrimeNGDropdown`, `JSDropdownElement` |
| `elements.tables` | `Table` and `NGXDataTable` for data-driven table verification |
| `steps` | All Cucumber step definitions — `BaseSteps`, `TextSteps`, `TableSteps`, `APISteps`, `ImageSteps`, `PDFVerificationSteps`, etc. |
| `webdrivers` | `Driver` singleton, `WebDriverFactory`, `WindowsDriverFactory`, `SentinelDriver`, Saucelabs/Grid factories |
| `apis` | API test support — `Request`/`Response` abstractions, `APIManager`, `APIFactory` |
| `system` | `FileManager`, `DownloadManager`, `TestManager`, `YAMLObject` (base for YAML-backed objects) |
| `configurations` | `Configuration` (reads `sentinel.yml`), `Time` (timeout helpers), `YAMLData` |
| `strings` | `SentinelStringUtils`, `AlphanumComparator` |
| `math` | `Decimal` for numeric comparisons with precision |
| `assertions` | `TableAssert` for table-specific assertions |
| `exceptions` | Custom exception types (`FileException`, `IOException`, `NoSuchActionException`, etc.) |
| `enums` | `SelectorType`, `PageObjectType`, `RequestType`, `HttpBodyType`, `YAMLObjectType` |

### Configuration System

Sentinel reads `conf/sentinel.yml` (not checked in; use `conf/example.sentinel.yml` as a template). Configuration is hierarchical — `default` settings are overridden by named environments (e.g., `stage`, `prod`). Key settings: `browser`, `headless`, `timeout`, `os`, `imageDirectory`, per-environment URLs and credentials.

### Element Selectors

YAML page objects define elements with one or more selector types tried in order:
```yaml
elements:
  login_button:
    xpath: "//button[@id='login']"
  email_field:
    css: "input.email"
    id: "email"
```
Supported `SelectorType` values: `CLASS`, `CSS`, `ID`, `NAME`, `XPATH`, `TEXT`, `PARTIALTEXT`.

### Cucumber Step Pattern

Step definitions use regex with optional articles for natural language:
```java
@When("^I click (?:the|a|an|on) (.*?)$")
public static void click(String elementName) {
    getElement(elementName).click();
}
```
Glue packages configured in `testng.xml`: `io.github.sentinel.steps`, `steps`, `hooks`.

### Logging

Uses Log4j2 via Lombok's `@Log` annotation. The `lombok.config` configures:
- Log field: `static private LOGGER`
- `@NonNull` throws `IllegalArgumentException`
- `@EqualsAndHashCode`/`@ToString` call super

### WebDriver Management

WebDriverManager (v5.9.2) auto-downloads matching browser drivers. All drivers are managed through `Driver` singleton; `Driver.quitAllDrivers()` closes all sessions at test teardown.

## Key Files

- `pom.xml` — dependencies, Java 17 compiler config, Surefire (TestNG), Jacoco, Javadoc, release plugins
- `testng.xml` — TestNG suite configuration; defines glue packages and Cucumber options
- `lombok.config` — Lombok behavior for the whole project
- `conf/example.sentinel.yml` — template for required runtime configuration
- `src/test/resources/cucumber.properties` — Cucumber runtime settings
- `src/test/resources/extent.properties` — Extent Reports HTML/PDF output paths

## Task Delegation

Spawn subagents to isolate context, parallelize independent work, or offload bulk mechanical tasks. Don't spawn when the parent needs the reasoning, when synthesis requires holding things together, or when spawn overhead dominates.

Pick the cheapest model that can do the subtask well:
- Haiku: bulk mechanical work, no judgment
- Sonnet: scoped research, code exploration, in-scope synthesis
- Opus: subtasks needing real planning or tradeoffs

If a subagent realizes it needs a higher tier than itself, return to the parent. Parent owns final output and cross-spawn synthesis. User instructions override.

## Preferred Tools

### Data Fetching

1. **WebFetch** — free, text-only, works on public pages that don't block bots.
2. **agent-browser CLI** — free, local Rust CLI + Chrome via CDP. For dynamic pages or auth walls that WebFetch can't handle. Returns the accessibility tree with element refs (@e1, @e2) — ~82% fewer tokens than screenshot-based tools. Install: `npm i -g agent-browser && agent-browser install`. Use `snapshot` for AI-friendly DOM state, element refs for interaction.
3. **Notice recurring fetch patterns and propose wrapping them as dedicated tools.** When the same fetch/parse logic comes up more than once, suggest wrapping it as a named tool (e.g. a skill file or a .py script that calls `agent-browser` with the snapshot and extraction steps baked in for that source). Add the entry to `## Dedicated Tools` below and reference it by name on future calls.

### PDF Files

Use 'pdftotext', not the 'Read' tool. Use 'Read' only when the user directly asks to analyze images or charts inside the document.

## Dedicated Tools

<!-- List project-specific tools here. For each, link to its skill or script file (e.g. `tools/reddit_fetch.py`). The orchestration logic lives in those files, not here. -->
