# Backend Run Guide

## Scope

This document explains how to run the Tax Workbench backend in local development.

## Prerequisites

- Java 21 target for the project
- Maven 3.9+

Note:

- The current local machine can run Maven with Java 23.
- The backend build itself is configured with `java.version=21`, so Java 21 is the intended baseline.

## Location

- Project root: `/Users/insu_han/IdeaProjects/example`
- Backend module: `/Users/insu_han/IdeaProjects/example/backend`

## Install and Run

From the backend directory:

```bash
cd /Users/insu_han/IdeaProjects/example/backend
mvn spring-boot:run
```

## Verification

Run tests:

```bash
cd /Users/insu_han/IdeaProjects/example/backend
mvn test
```

Check the info endpoint:

```bash
curl http://localhost:8080/api/info
```

Check actuator health:

```bash
curl http://localhost:8080/actuator/health
```

## Current Defaults

- Port: `8080`
- Database: in-memory H2
- H2 console: enabled

Main configuration file:

- `/Users/insu_han/IdeaProjects/example/backend/src/main/resources/application.yml`

## Known Notes

- `spring.jpa.hibernate.ddl-auto` is currently `none` because Step 1 only sets up the skeleton.
- Real schema creation should be added after the Step 2 domain and JPA model are introduced.
