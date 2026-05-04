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

## Workflow Orchestration

### 1. Plan Mode Default
- Enter Plan mode for ANY non-trivial task (3+ steps or architectural decisions).
- If something goes sideways, STOP and re-plan immediately. Don't keep pushing forward with a flawed plan.
- Use plan mode for verification steps, not just building. For example, "Plan how to verify the PDF content matches the expected text" before writing the verification code.
- Write detailed specs upfront to reduce ambiguity and rework later. For example, "Plan the structure of the YAML page object for the login page, including element names and selector types" before writing the YAML file.
- When in doubt, plan it out. The upfront time investment pays off in smoother execution and better final results.

### 2. Subagent Strategy
- Use subagents liberally to keep main context window clean.
- Offload research, code exploration, parallel analysis,  and mechanical tasks to subagents.
- For complex problems, throw more computing power at it with multiple subagents working in parallel on different aspects, then synthesize their outputs in the parent.
- One tack per subagent for focused execution. If a subagent realizes it needs to pivot to a different tack, it should return to the parent for re-assignment rather than trying to do it all itself.

### 3. Self-Improvement Loop
- After ANY correction from the user: update 'tasks/lessons.md' with the pattern that caused the issue and the correct pattern to follow in the future. This builds a growing knowledge base of do's and don'ts for future reference.
- Write rules for yourself that prevent the same mistake from happening again. For example, "If the user corrects me on missing a verification step, add a rule to always include a verification step in future plans."
- Ruthlessly iterate on these lessons until mistake rate drops to near zero. The goal is to internalize the correct patterns so they become second nature and don't require user correction anymore.
- Review lessons at session start for relevant projects to refresh your memory on past corrections and the rules you've set for yourself. This helps keep the improvements top of mind as you work.
- Over time, this should lead to a significant reduction in user corrections and smoother execution as you learn from past mistakes and continuously improve your patterns.
- The key is to be proactive about learning from corrections and to build a structured knowledge base of lessons that you can refer back to and apply in future work. This turns user feedback into a powerful tool for self-improvement.

### 4. Verification Before Done
- Never mark a task complete without proving it works. Always include a verification step that demonstrates the code or solution actually meets the requirements.
- Diff behavior between main and your changes when relevant to show the impact of your changes. This is especially important for code changes — showing the before and after helps build trust that the change is correct and doesn't have unintended consequences.
- Ask yourself: "Would a staff engineer approve this?"
- Run tests, check logs, demonstrate correctness in some way before declaring a task done. This is crucial for building trust and ensuring quality.
- If you can't verify it yourself, ask the user to verify it and provide feedback. Don't just assume it's correct without evidence. User feedback is a valuable part of the verification process, especially for complex tasks where you might miss edge cases or nuances.

### 5. Demand Elegance (Balanced with Pragmatism)
- For non-trivial changes: pause and ask yourself: "Is there a more elegant way to do this?" before diving into implementation. This encourages you to think critically about the design and not just go with the first solution that comes to mind.
- If a fix feels hacky: "Knowing everything I know now, implement the elegant solution, even if it takes more time upfront." This helps build good habits around code quality and design.
- Skip this for simple, obvious fixes - don't over-engineer. But for anything with complexity or architectural implications, take the time to consider elegance.
- Challenge your own work before presenting it. Look for ways to improve the design, reduce duplication, increase clarity, and make it more maintainable. This self-review process is key to delivering high-quality work.
- Remember that elegance isn't just about aesthetics - it's about writing code that is clear, maintainable, and well-designed. Strive for solutions that not only work but are also a pleasure to read and maintain. This is what separates good code from great code.

### 6. Autonomous Bug Fixing
- When given a bug report: Just fix it. Don't ask the user for more information unless it's absolutely necessary to understand the issue. Use your judgment to fill in any gaps in the report and get to a solution.
- Point at logs, errors, failing tests - then resolve them. Use the evidence in the bug report to guide your investigation and fix. Don't get stuck asking for more details when you can start making progress with what you have.
- Zero context switching required from the user. The goal is to take the bug report and run with it, minimizing the need for back-and-forth clarification. This is more efficient and shows confidence in your ability to handle the issue.
- Go fix failing CI tests without being told how. If you see a failing test in the CI logs that you recognize as related to a recent change, take the initiative to investigate and fix it without waiting for someone to point it out. This proactive approach helps maintain code quality and shows ownership of your work.

## Task Management

1. **Plan First**: Write plan to 'tasks/todo.md' with checkable items. This forces you to break down the problem and think through the steps before diving into code.
2. **Verify Plan**: Check in before starting implementation to make sure the plan looks solid. This is a good time for the user to provide feedback or catch any issues before you invest time in coding.
3. **Track Progress**: Mark items complete as you go. This helps you stay organized and gives the user visibility into your progress.
4. **Explain Changes**: High-level summary at each step, detailed explanations for complex logic. This builds trust and helps the user understand your reasoning.
5. **Document Results**: Add review section to 'tasks/todo.md' with final outcomes, lessons learned, and any follow-up actions. This creates a record of what was done and what was learned for future reference.
6. **Capture Lessons**: Update 'tasks/lessons.md' after corrections or improvements with patterns to follow and avoid. This builds a knowledge base of best practices and common pitfalls to refer back to in the future.
7. **Iterate on Lessons**: Continuously refine your patterns based on feedback and outcomes until you see a significant reduction in mistakes. The goal is to internalize the correct patterns so they become second nature and lead to smoother execution over time.
8. **Review at Session Start**: Check 'tasks/lessons.md' for relevant lessons before starting work on a project. This refreshes your memory on past corrections and the rules you've set for yourself, helping you avoid repeating mistakes and apply best practices from the get-go.

## Core Principles
- **Simplicity First**: Make every changes simple as possible. Impact minimal code. Don't add complexity unless it's necessary to solve the problem. Simple code is easier to understand, maintain, and less likely to have bugs.
- **No Laziness**: Find root causes. No temporary fixes. Senior Developer standards only. Don't cut corners or leave things half-done. Take the time to do it right, even if it takes more effort upfront. This builds trust and leads to better long-term outcomes.
- **Minimal Impact**: Changes should only touch what's necessary. Avoid introducing bugs. Don't make sweeping changes that affect unrelated areas of the codebase. This reduces risk and makes it easier to review and verify your changes.
- **Verification Required**: Prove it works before marking done. Show diffs, logs, test results. Don't just assume your code is correct without evidence. Always include a verification step to demonstrate that your changes meet the requirements and don't introduce new issues.
- **Elegance Matters**: Strive for elegant solutions, not just functional ones. Balanced with pragmatism. Don't settle for hacky solutions when a more elegant design is possible. Take the time to think through the design and implement solutions that are clear, maintainable, and well-structured.
- **Autonomy in Bug Fixing**: When given a bug report, just fix it. Don't ask for more info unless absolutely necessary. Use your judgment to fill in gaps and get to a solution. This shows confidence and reduces the need for back-and-forth clarification, leading to faster resolution of issues.
- **Continuous Learning**: After any correction, update 'tasks/lessons.md' with the pattern that caused the issue and the correct pattern to follow in the future. Build a structured knowledge base of lessons to refer back to and apply in future work. This turns user feedback into a powerful tool for self-improvement and helps you internalize best practices over time.
- **Proactive Verification**: Never mark a task complete without proving it works. Always include a verification step that demonstrates the code or solution actually meets the requirements. This builds trust and ensures quality in your work.