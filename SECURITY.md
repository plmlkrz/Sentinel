# Security Policy

## Supported Versions

Only the latest release of Sentinel receives security fixes. We do not backport patches to older versions.

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x (latest) | ✅ Yes      |
| < 1.0   | ❌ No              |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

We use [GitHub Private Security Advisories](https://github.com/sentinel-framework/sentinel/security/advisories/new) to handle vulnerability disclosures confidentially. This keeps details out of the public record until a fix is ready and gives us a chance to coordinate a responsible disclosure.

### What to include

A good report helps us triage and fix faster. Please provide:

- A clear description of the vulnerability and its potential impact
- Steps to reproduce (proof-of-concept code or a minimal test case is ideal)
- Affected version(s) and environment (OS, Java version, browser/driver if relevant)
- Any suggested fix or mitigation, if you have one

### What to expect

- **Acknowledgment** within 3 business days confirming we received your report
- **Initial assessment** within 7 business days — we'll confirm whether it's a valid vulnerability and share our planned timeline
- **Fix and disclosure** coordinated with you before any public announcement

We won't take legal action against researchers who report responsibly and follow this policy.

## Scope

Sentinel is a test automation framework — a developer tool, not a production application. That said, some security considerations are relevant:

**In scope:**
- Vulnerabilities in Sentinel's own code that could compromise a CI/CD pipeline or test environment
- Dependency vulnerabilities in Sentinel that expose downstream consumers (projects that pull Sentinel from Maven Central)
- Credential or secret exposure risks in framework configuration handling

**Out of scope:**
- Vulnerabilities in the applications under test (report those to the respective project)
- Issues that require physical access to the machine running tests
- Theoretical vulnerabilities with no realistic attack path in a test automation context

## Dependency Vulnerabilities

Sentinel publishes a signed artifact to Maven Central. If you discover a CVE in one of Sentinel's transitive dependencies, please report it so we can assess impact and update the dependency as needed. Include the CVE identifier and which Sentinel version is affected.

## Attribution

We're happy to credit researchers in the release notes or CHANGELOG for valid, responsibly disclosed vulnerabilities. Let us know in your report if you'd like to be credited and how.

