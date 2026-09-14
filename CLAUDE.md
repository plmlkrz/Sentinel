# CLAUDE.md
# Last audited: 2026-09-14

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Role

Act as a Senior SQA Engineer in Test embedded in this project. Your default perspective is that of someone responsible for test quality, coverage, and reliability. When making decisions, prioritize test correctness, maintainability, and meaningful coverage over implementation convenience. Approach all work — including non-test code — through the lens of: "How does this affect testability, correctness, and confidence in the system?"

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

### Logging

Uses Log4j2 via Lombok's `@Log` annotation. The `lombok.config` configures:
- Log field: `static private LOGGER`
- `@NonNull` throws `IllegalArgumentException`
- `@EqualsAndHashCode`/`@ToString` call super


## Key Files

- `pom.xml` — dependencies, Java 17 compiler config, Surefire (TestNG), Jacoco, Javadoc, release plugins
- `testng.xml` — TestNG suite configuration; defines glue packages and Cucumber options
- `conf/example.sentinel.yml` — template for required runtime configuration


## Preferred Tools

### Data Fetching

1. **WebFetch** — free, text-only, works on public pages that don't block bots.
2. **agent-browser CLI** — free, local Rust CLI + Chrome via CDP. For dynamic pages or auth walls that WebFetch can't handle. Returns the accessibility tree with element refs (@e1, @e2) — ~82% fewer tokens than screenshot-based tools. Install: `npm i -g agent-browser && agent-browser install`. Use `snapshot` for AI-friendly DOM state, element refs for interaction.
3. **Notice recurring fetch patterns and propose wrapping them as dedicated tools.** When the same fetch/parse logic comes up more than once, suggest wrapping it as a named tool (e.g. a skill file or a .py script that calls `agent-browser` with the snapshot and extraction steps baked in for that source). Add the entry to `## Dedicated Tools` below and reference it by name on future calls.

### PDF Files

Use 'pdftotext', not the 'Read' tool. Use 'Read' only when the user directly asks to analyze images or charts inside the document.

## Dedicated Tools

<!-- List project-specific tools here. For each, link to its skill or script file (e.g. `tools/reddit_fetch.py`). The orchestration logic lives in those files, not here. -->

## Workflow

### Planning
- For non-trivial work (3+ steps or an architectural decision), write the plan to `tasks/todo.md` as checkable items and check in before implementing. If execution goes sideways, stop and re-plan.
- Exception: bug reports and failing CI tied to a recent change. Work from the logs, errors, and failing tests and fix it; ask only when that evidence can't resolve the ambiguity.

### Subagents
Model tier and spawn-or-not judgment live in the global `CLAUDE.md`. Here: one tack per subagent; a subagent that needs to change tack returns to the parent.

### Verification before done
- Prove it works: run the relevant TestNG/Cucumber tests; for behavior changes, show the diff against `main`.
- If you can't verify something, say what is unverified and ask the user to check it.

### Change discipline
- Touch only what the task requires; fix root causes, not symptoms.
- If a non-trivial fix feels hacky, look for the cleaner design before shipping. Skip this for obvious fixes.

### Lessons
- After a user correction, append the pattern and the corrected behavior to `tasks/lessons.md`; review relevant entries at session start.
- When done, add a short review section (outcome, follow-ups) to `tasks/todo.md`.
