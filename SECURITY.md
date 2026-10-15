# Security policy

CoverCompare is a portfolio project, and every insurer and customer in it is fictional. It still
follows normal security practice, and reports are welcome.

## Reporting a vulnerability

Please report it privately using GitHub's
[private vulnerability reporting](https://github.com/DDKPhamm/covercompare/security/advisories/new),
not as a public issue. You should get a reply within a week.

## What is checked automatically

Every pull request, every merge to `main`, and once a week:

- **CodeQL** analyses the Java code for security flaws.
- **Gitleaks** scans the full git history for committed secrets. GitHub secret scanning with
  push protection also blocks known secret formats from being pushed at all.
- **Trivy** scans the Dockerfile (and, from Phase 3, the Terraform) for insecure configuration.
- **Trivy** scans each service's container image, including every Java library in it, and fails
  the build on high or critical vulnerabilities that have a fix available. A CycloneDX SBOM
  (software bill of materials) is kept for every image.
- **Dependabot** opens pull requests for outdated Maven dependencies, GitHub Actions and base images.
