# interceptJ

> State-of-the-art fraud detection for the JVM.

`interceptJ` is a composable, type-safe fraud detection library that lets you plug in any number of risk signals — IP reputation, device fingerprints, velocity checks, ML scores — and reduce them to a single, actionable verdict in a clean, fluent pipeline.

```java
Optional<Response> response = Interceptor.interceptor()
    .detect(request.getIpAddress(),   ipReputationDetector)
    .detect(request.getDeviceId(),    deviceFingerprintDetector)
    .detect(request.getUserId(),      velocityDetector)
    .decide(fraudDecider)
    .onBlock(()     -> Response.status(403).build())
    .onChallenge(() -> Response.status(429).header("X-Challenge", "captcha").build())
    .onDefer(()     -> Response.status(202).entity("Under review").build())
    .onProceed(()   -> service.handle(request))
    .result();
```

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Core Concepts](#core-concepts)
- [Usage](#usage)
  - [Implementing a Detector](#implementing-a-detector)
  - [Implementing a Decider](#implementing-a-decider)
  - [Running the Pipeline](#running-the-pipeline)
  - [Conditional Detectors](#conditional-detectors)
  - [Decision Detail](#decision-detail)
- [Modules](#modules)
- [Building from Source](#building-from-source)
- [License](#license)

---

## Features

- **Detect-then-decide pipeline** — cleanly separates signal collection from policy enforcement.
- **Composable detectors** — register as many detectors as you need; each runs independently.
- **Conditional execution** — wrap any detector in a runtime condition to skip expensive calls when they are not needed.
- **Four verdict types** — `BLOCK`, `PROCEED`, `CHALLENGE`, and `DEFER` cover the full spectrum of fraud responses.
- **Type-safe result handling** — fluent `onBlock` / `onProceed` / `onChallenge` / `onDefer` handlers return a typed `Optional<R>`; only the matching handler fires.
- **Audit metadata** — attach structured `DecisionDetail` to any verdict for logging and compliance.
- **Zero framework coupling** — plain Java 25 with no mandatory runtime dependencies beyond Jakarta Annotations.

---

## Requirements

- Java 25 or later
- Gradle 8+ (for building from source)

---

## Installation

### Using the BOM (recommended)

Import the Bill of Materials to align all `interceptJ` dependency versions automatically.

**Gradle (Kotlin DSL)**

```kotlin
dependencies {
    implementation(platform("io.unconquerable:interceptJ-bom:0.0.1-SNAPSHOT"))
    implementation("io.unconquerable:interceptJ-core")
}
```

**Gradle (Groovy DSL)**

```groovy
dependencies {
    implementation platform("io.unconquerable:interceptJ-bom:0.0.1-SNAPSHOT")
    implementation "io.unconquerable:interceptJ-core"
}
```

**Maven**

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.unconquerable</groupId>
            <artifactId>interceptJ-bom</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.unconquerable</groupId>
        <artifactId>interceptJ-core</artifactId>
    </dependency>
</dependencies>
```

---

## Core Concepts

The library is built around five types that work together in a linear pipeline.

```
┌─────────────────────────────────────────────────────────────┐
│                        Interceptor                          │
│                                                             │
│  detect(target, Detector) ──► Detected                      │
│  detect(target, Detector) ──► Detected  ─┐                  │
│  detect(target, Detector) ──► Detected   │                  │
│                                          ▼                  │
│                               Decider(List<Detected>)        │
│                                          │                  │
│                                          ▼                  │
│                                       Decided               │
│                                (BLOCK | PROCEED |           │
│                                 CHALLENGE | DEFER)          │
│                                          │                  │
│                                          ▼                  │
│                                    Decision<R>              │
│                           .onBlock / .onProceed / ...       │
└─────────────────────────────────────────────────────────────┘
```

| Type | Role |
|---|---|
| `Interceptor` | Entry point; holds detector registrations and drives the pipeline |
| `Detector<T>` | Analyses a single target value and returns a `Detected` result |
| `Detected` | The output of one detector — either a `DetectedScore` or a `DetectedStatus` |
| `Decider` | Examines all `Detected` results and returns a `Decided` verdict |
| `Decision<R>` | Maps each verdict type to a caller-supplied handler and returns the result |

### Detection result types

| Type | When to use |
|---|---|
| `DetectedScore` | The detector produces a continuous risk score (e.g. ML confidence, velocity count) |
| `DetectedStatus` | The detector produces a discrete signal — `DETECTED`, `NOT_DETECTED`, or `SKIPPED` |

### Verdict types

| Verdict | Meaning |
|---|---|
| `BLOCK` | Reject outright — risk signals exceed the acceptable threshold |
| `PROCEED` | Allow — all signals within acceptable bounds |
| `CHALLENGE` | Require additional verification (CAPTCHA, OTP, step-up auth) before proceeding |
| `DEFER` | Hold for asynchronous review by a human analyst or downstream process |

---

## Usage

### Implementing a Detector

Implement `Detector<T>` for each risk signal you want to evaluate. Return a `DetectedScore` for quantitative signals or a `DetectedStatus` for binary ones.

```java
public class IpReputationDetector implements Detector<String> {

    private final IpReputationService service;

    @Override
    public String name() {
        return "ip-reputation";
    }

    @Override
    public Detected detect(String ipAddress) {
        BigDecimal score = service.riskScoreOf(ipAddress);
        return new DetectedScore(name(), score);
    }
}
```

```java
public class DeviceBlocklistDetector implements Detector<String> {

    private final BlocklistService blocklist;

    @Override
    public String name() {
        return "device-blocklist";
    }

    @Override
    public Detected detect(String deviceId) {
        boolean blocked = blocklist.contains(deviceId);
        return new DetectedStatus(name(), blocked
                ? DetectedStatus.Status.DETECTED
                : DetectedStatus.Status.NOT_DETECTED);
    }
}
```

---

### Implementing a Decider

`Decider` is a `@FunctionalInterface`. Implement it as a lambda for simple policies or as a class for reusable, testable strategies.

**Lambda — block on any DETECTED signal**

```java
Decider blockOnAnySignal = detections -> detections.stream()
    .filter(d -> d instanceof DetectedStatus ds
            && ds.status() == DetectedStatus.Status.DETECTED)
    .findFirst()
    .<Decided>map(_ -> Decided.decidedToBlock())
    .orElse(Decided.decidedToProceed());
```

**Class — score-based tiered policy**

```java
public class TieredScoreDecider implements Decider {

    private static final BigDecimal BLOCK_THRESHOLD     = new BigDecimal("0.75");
    private static final BigDecimal CHALLENGE_THRESHOLD = new BigDecimal("0.40");

    @Override
    public Decided decide(List<Detected> detections) {
        // Hard block if any device is on the blocklist
        boolean deviceBlocked = detections.stream()
            .filter(d -> d instanceof DetectedStatus ds
                    && "device-blocklist".equals(ds.detectorName()))
            .map(d -> (DetectedStatus) d)
            .anyMatch(d -> d.status() == DetectedStatus.Status.DETECTED);

        if (deviceBlocked) {
            return Decided.decidedToBlock(new FraudDetail("DEVICE_BLOCKLISTED"));
        }

        // Aggregate risk scores from remaining detectors
        BigDecimal totalScore = detections.stream()
            .filter(d -> d instanceof DetectedScore)
            .map(d -> ((DetectedScore) d).score())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalScore.compareTo(BLOCK_THRESHOLD)     > 0) return Decided.decidedToBlock();
        if (totalScore.compareTo(CHALLENGE_THRESHOLD) > 0) return Decided.decidedToChallenge();
        return Decided.decidedToProceed();
    }
}
```

---

### Running the Pipeline

Wire detectors and a decider together with `Interceptor`:

```java
Optional<ApiResponse> response = Interceptor.interceptor()
    .detect(request.getIpAddress(), new IpReputationDetector(ipService))
    .detect(request.getDeviceId(),  new DeviceBlocklistDetector(blocklist))
    .decide(new TieredScoreDecider())
    .onBlock(()     -> ApiResponse.forbidden("Request blocked"))
    .onChallenge(() -> ApiResponse.challenge("Complete verification to continue"))
    .onDefer(()     -> ApiResponse.accepted("Your request is under review"))
    .onProceed(()   -> orderService.submit(request))
    .result();
```

Only one handler fires — whichever matches the verdict. If no handler is registered for the active verdict, `result()` returns `Optional.empty()`.

---

### Conditional Detectors

Wrap any detector with `Detectors.detector(...)` to attach a runtime condition. When the condition evaluates to `false`, the detector is skipped and a `SKIPPED` status is returned in its place — preserving the detector's slot in the result list without producing a false negative.

```java
import io.ununconquerable.intercept.Interceptor;
import static io.unconquerable.intercept.detect.Detectors.detector;

Interceptor.interceptor()
    // Only run the 3-D Secure check for card payments
    .detect(request.getCardNumber(),
            detector(threeDSecureDetector)
                .when(() -> request.getPaymentMethod() == PaymentMethod.CARD)
                .build())
    // Only run velocity check for authenticated users
    .detect(request.getUserId(),
            detector(velocityDetector)
                .when(() -> request.isAuthenticated())
                .and(() -> featureFlags.isVelocityCheckEnabled())
                .build())
    .decide(decider)
    ...
```

Conditions are lazily evaluated at pipeline execution time and support `when()`, `and()`, and `or()` composition with standard short-circuit semantics.

---

### Decision Detail

Attach structured metadata to any verdict for audit trails, logging, or compliance reporting. Implement `DecisionDetail` with a record:

```java
public record FraudDetail(String ruleId, String detectorName, BigDecimal score)
        implements DecisionDetail {}
```

Return it from a `Decider`:

```java
return Decided.decidedToBlock(new FraudDetail("VELOCITY_EXCEEDED", "velocity", score));
```

Access it in an outcome handler:

```java
.onBlock(() -> {
    decided.details()
           .filter(d -> d instanceof FraudDetail)
           .map(d -> (FraudDetail) d)
           .ifPresent(d -> auditLog.record(d.ruleId(), d.detectorName()));
    return Response.status(403).build();
})
```

---

## Modules

| Module | Artifact | Description |
|---|---|---|
| `interceptJ-core` | `io.unconquerable:interceptJ-core` | All library types — detectors, deciders, pipeline |
| `interceptJ-bom` | `io.unconquerable:interceptJ-bom` | Bill of Materials for version alignment |

---

## Building from Source

```bash
# Clone the repository
git clone https://github.com/unconquerableIO/interceptJ.git
cd interceptJ

# Build and run all tests
./gradlew build

# Generate Javadoc
./gradlew :interceptJ-core:javadoc

# Install to local Maven repository
./gradlew publishToMavenLocal
```

Javadoc is written to `interceptJ-core/build/docs/javadoc/`.

---

## License

Licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) (the "License");
you may not use this software except in compliance with the License.
