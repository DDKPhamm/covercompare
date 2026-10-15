# 11. Security scanning in the pipeline

Date: 2026-10-15
Status: Accepted

## Context

Before deploying to the public internet, the project needs to catch security problems
automatically: flaws in our own code, secrets committed by mistake, insecure Docker or cloud
configuration, and known vulnerabilities in the libraries and base images we ship. Checking by
hand does not happen reliably, and new vulnerabilities are published against code that has not
changed.

## Decision

A separate `Security` workflow runs on every pull request, on every merge to `main`, and weekly:

- **CodeQL** (`security-extended` queries) for static analysis of the Java code.
- **Gitleaks** over the full git history, because a secret deleted in a later commit is still
  leaked. GitHub secret scanning with push protection is also enabled, to stop known secret
  formats before they reach the repository.
- **Trivy configuration scan** of the repository: the Dockerfile now, Terraform from Phase 3.
- **Trivy image scan** of every service's container image. The build fails on HIGH or CRITICAL
  vulnerabilities **that have a fix available**. A CycloneDX SBOM is stored for every image.

Third-party actions are pinned to a full commit SHA rather than a tag, because a tag can be moved
to malicious code. Dependabot keeps the pins up to date.

## Consequences

- A vulnerable dependency or base image blocks merging until it is upgraded, or explicitly
  accepted in `.trivyignore` with a written reason and review date.
- Vulnerabilities with no fix yet are reported but do not block, since nothing can be done except
  wait for a patch; the weekly run picks up the fix when it appears.
- Major Java and PostgreSQL upgrades are excluded from Dependabot and done deliberately, because
  they change runtime behaviour rather than patch it.
