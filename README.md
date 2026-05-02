# interceptJ

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/Java-25-orange.svg?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)

> Fraud detection using Java .

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
  - [Instrument Types](#instrument-types)
  - [Forwarding Pipeline Results (Sender)](#forwarding-pipeline-results-sender)
- [Modules](#modules)
- [Building from Source](#building-from-source)
- [License](#license)

---

## Features

- **Detect-then-decide pipeline** — cleanly separates signal collection from policy enforcement.
- **Composable detectors** — register as many detectors as you need; each runs independently.
- **Conditional execution** — wrap any detector in a runtime condition to skip expensive calls when they are not needed.
- **Four verdict types** — `BLOCK`, `PROCEED`, `CHALLENGE`, and `DEFER` cover the full spectrum of fraud responses.
- **Type-safe result handling** — fluent `onBlock` / `onProceed` / `onChallenge` / `onDefer` handlers return a typed `Optional<R>`; only the matching handler fires. Each verdict also has a `Runnable` overload for side-effect-only handling (logging, metrics, events) with no return value.
- **Audit metadata** — attach structured `DecisionDetail` to any verdict for logging and compliance.
- **Instrument identification** — model the subject of a pipeline run as an `InstrumentType` (credit card, IP address, device) and attach it to a `InstrumentIdentifier` that bundles tenant, user, and instrument into one typed value.
- **Pipeline forwarding** — register a `Sender` to dispatch the full pipeline context — verdict, result, detections, instrument identifier, and arbitrary metadata — to audit logs, message queues, or monitoring systems in one call. Conditional variants (`sendOnBlock`, `sendUnlessProceed`, etc.) target specific verdicts.
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
    implementation(platform("io.unconquerable:interceptJ-bom:0.0.1"))
    implementation("io.unconquerable:interceptJ-core")
}
```

**Gradle (Groovy DSL)**

```groovy
dependencies {
    implementation platform("io.unconquerable:interceptJ-bom:0.0.1")
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
            <version>0.0.1</version>
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

The library is built around eight types that work together in a linear pipeline.

```
┌─────────────────────────────────────────────────────────────┐
│                        Interceptor                          │
│                                                             │
│  detect(target, Detector) ──► Detected                      │
│  detect(target, Detector) ──► Detected  ─┐                  │
│  detect(target, Detector) ──► Detected   │                  │
│                                          ▼                  │
│                               Decider(List<Detected>)       │
│                                          │                  │
│                                          ▼                  │
│                                       Decided               │
│                                (BLOCK | PROCEED |           │
│                                 CHALLENGE | DEFER)          │
│                                          │                  │
│                                          ▼                  │
│                                    Decision<R>              │
│                           .onBlock / .onProceed / ...       │
│                                          │                  │
│                             .send(Sender) ─► audit / queue  │
└─────────────────────────────────────────────────────────────┘
```

| Type | Role |
|---|---|
| `Interceptor` | Entry point; holds detector registrations and drives the pipeline |
| `Detector<T>` | Analyses a single target value and returns a `Detected` result |
| `Detected<T>` | The output of one detector — either a `DetectedScore` or a `DetectedStatus` |
| `Decider` | Examines all `Detected` results and returns a `Decided` verdict |
| `Decision<R>` | Maps each verdict type to a caller-supplied `Supplier<R>` or `Runnable` handler and returns the result |
| `Sender<R>` | Receives the complete pipeline context — result, verdict, detections, identifier, and metadata — for forwarding to external systems |
| `InstrumentType` | Marker interface for the domain object being evaluated (credit card, IP address, device fingerprint, etc.) |
| `InstrumentIdentifier<T>` | Bundles tenant (`accountId`), owner (`userId`), and the typed instrument into one value that can be attached to a `Sender` call |

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
    public Detected<String> detect(String ipAddress) {
        BigDecimal score = service.riskScoreOf(ipAddress);
        return new DetectedScore<>(name(), ipAddress, score);
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
    public Detected<String> detect(String deviceId) {
        boolean blocked = blocklist.contains(deviceId);
        return new DetectedStatus<>(name(), deviceId, blocked
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
    public Decided decide(List<Detected<?>> detections) {
        // Hard block if any device is on the blocklist
        boolean deviceBlocked = detections.stream()
            .filter(d -> d instanceof DetectedStatus<?> ds
                    && "device-blocklist".equals(ds.detectorName()))
            .map(d -> (DetectedStatus<?>) d)
            .anyMatch(d -> d.status() == DetectedStatus.Status.DETECTED);

        if (deviceBlocked) {
            return Decided.decidedToBlock(new FraudDetail("DEVICE_BLOCKLISTED"));
        }

        // Aggregate risk scores from remaining detectors
        BigDecimal totalScore = detections.stream()
            .filter(d -> d instanceof DetectedScore<?>)
            .map(d -> ((DetectedScore<?>) d).score())
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
    .onChallenge(() -> ApiResponse.status(401).header("WWW-Authenticate", "OTP").body("Complete verification to continue"))
    .onDefer(()     -> ApiResponse.accepted("Your request is under review"))
    .onProceed(()   -> orderService.submit(request))
    .result();
```

Only one handler fires — whichever matches the verdict. If no handler is registered for the active verdict, `result()` returns `Optional.empty()`.

#### Side-effect handlers

Every verdict also accepts a `Runnable` overload for cases where you need to react to an outcome — emit a metric, write an audit log, publish an event — without producing a return value. `result()` returns `Optional.empty()` when only a `Runnable` handler fires.

```java
interceptor()
    .detect(request.getIpAddress(), ipDetector)
    .decide(decider)
    .onBlock(() -> auditLog.record("blocked", request.getIpAddress()))  // Runnable — no return
    .onBlock(() -> ApiResponse.forbidden("Request blocked"))             // Supplier — returns value
    .onProceed(() -> orderService.submit(request))
    .result();
```

Both overloads can target the same verdict. The `Runnable` fires for its side effect; the `Supplier` sets `result()`. Mixing them on different verdicts is equally valid:

```java
interceptor()
    .detect(request.getUserId(), velocityDetector)
    .decide(decider)
    .onBlock(() -> metrics.increment("fraud.blocked"))   // side effect only
    .onChallenge(() -> metrics.increment("fraud.challenged"))
    .onProceed(() -> orderService.submit(request))       // produces a result
    .result();
```

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

### Instrument Types

`InstrumentType` is a marker interface for the domain object being evaluated — a credit card, an IP address, a device fingerprint, or any other subject. `InstrumentIdentifier<T>` wraps it together with the tenant and user context.

**Define your instrument**

```java
public record CreditCard(String number, String holder) implements InstrumentType {
    public String type() { return "credit-card"; }
}
```

**Define your identifier**

```java
public record PaymentIdentifier(String accountId, String userId, CreditCard instrument)
        implements InstrumentIdentifier<CreditCard> {}
```

**Use it in the pipeline**

The identifier is passed to `.send()` overloads so senders receive full tenant and instrument context alongside the verdict. The identifier is supplied lazily so it is only evaluated when the send condition is met.

```java
var identifier = new PaymentIdentifier("acct-42", "user-99", card);

Interceptor.interceptor()
    .detect(card, fraudDetector)
    .decide(decider)
    .onBlock(()   -> ApiResponse.forbidden("Card blocked"))
    .onProceed(() -> paymentService.charge(card))
    .send(auditSender, () -> identifier)       // forwarded to all senders
    .sendOnBlock(alertSender, () -> identifier, Map.of("reason", "fraud"))
    .result();
```

---

### Forwarding Pipeline Results (Sender)

After all outcome handlers have been registered, call `.send()` to dispatch the complete pipeline context — the handler result, the verdict, every detection signal, the instrument identifier, and any caller-supplied metadata — to an external system in a single step. `Sender<R>` is a `@FunctionalInterface`, so it can be supplied as a lambda.

```java
Sender<ApiResponse> auditSender = (result, decided, detections, identifier, metadata) ->
    auditLog.record(AuditEntry.builder()
        .verdict(decided.type())
        .detections(detections)
        .accountId(identifier != null ? identifier.accountId() : null)
        .response(result)
        .build());
```

Wire it into the pipeline after the outcome handlers:

```java
Optional<ApiResponse> response = Interceptor.interceptor()
    .detect(request.getIpAddress(), ipDetector)
    .detect(request.getDeviceId(),  deviceDetector)
    .decide(decider)
    .onBlock(()     -> ApiResponse.forbidden("Request blocked"))
    .onChallenge(() -> ApiResponse.status(401).header("WWW-Authenticate", "OTP").body("Verification required"))
    .onProceed(()   -> orderService.submit(request))
    .send(auditSender)   // fires unconditionally; receives result + verdict + detections
    .result();
```

`send()` fires regardless of which verdict matched, making it the right place for unconditional audit logging, metrics, or event publishing. The `result` parameter is the value set by the matching handler, or `null` if a `Runnable` handler was used or no handler was registered.

#### Enriching the sender with identifier and metadata

Every `.send()` variant accepts an optional `InstrumentIdentifier` supplier and/or a `Map<String, Object>` metadata map. The identifier supplier is evaluated lazily — only when the send condition is met.

```java
var identifier = new PaymentIdentifier("acct-42", "user-99", card);
var meta       = Map.<String, Object>of("traceId", requestId, "channel", "web");

.send(auditSender, () -> identifier)            // identifier only
.send(metricsSender, meta)                      // metadata only
.send(eventBusSender, () -> identifier, meta)   // both
.result();
```

#### Conditional send variants

Use the conditional variants to target a specific verdict or exclude one:

| Method | Fires when |
|---|---|
| `sendOnBlock(sender)` | verdict is `BLOCK` |
| `sendOnProceed(sender)` | verdict is `PROCEED` |
| `sendOnChallenge(sender)` | verdict is `CHALLENGE` |
| `sendOnDefer(sender)` | verdict is `DEFER` |
| `sendUnlessBlocked(sender)` | verdict is **not** `BLOCK` |
| `sendUnlessProceed(sender)` | verdict is **not** `PROCEED` |
| `sendUnlessDefer(sender)` | verdict is **not** `DEFER` |

All variants accept the same `(sender)`, `(sender, identifier)`, `(sender, metadata)`, and `(sender, identifier, metadata)` overloads shown above.

```java
.sendOnBlock(fraudAlertSender, () -> identifier, Map.of("reason", "high-risk"))
.sendUnlessProceed(anomalySender, () -> identifier)
.result();
```

You can register multiple senders by chaining calls:

```java
.send(auditSender)
.send(metricsSender)
.send(eventBusSender)
.result();
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
