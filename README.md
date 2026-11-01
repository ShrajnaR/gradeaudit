# GradeAudit

[![CI](https://github.com/ShrajnaR/gradeaudit/actions/workflows/ci.yml/badge.svg)](https://github.com/ShrajnaR/gradeaudit/actions/workflows/ci.yml)

An inspector for AI graders. Some organisations let an LLM grade how a candidate worked with an AI assistant. GradeAudit tests whether that grader is steady, ignores irrelevant changes, and notices real differences, and it reports the evidence.

**Status:** in development.

## How it works

1. Generate practice sessions from a seed.
2. Ask the grader to grade each one several times.
3. Grade harmless variants (typos, a different name) and harmful variants (test runs removed).
4. Compute four checks and a verdict, with the unstable samples as evidence.

## Run locally

Requires Java 17 and Docker.

Start the local database (development credentials only, never for production):

    docker compose up -d

Run the tests (Windows PowerShell; on macOS or Linux use ./mvnw):

    .\mvnw.cmd verify

Health check once the app runs: http://localhost:8080/actuator/health

## Scope and honesty

GradeAudit applies known judge-audit methods to graders of AI-collaboration sessions. It uses generated data only, and it is not a legal bias audit. It does not audit any vendor's product.

## License

MIT