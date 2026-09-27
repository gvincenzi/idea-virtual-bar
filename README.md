# ☕ IDEA - Intent Driven Event Architecture (Virtual Bar)
<img src="src/main/resources/static/images/logo.jpg" width="200">

[![Java 21 LTS](https://img.shields.io/badge/Java-21%20LTS-blue.svg)](https://openjdk.org/)
[![Spring Boot 3.5.5](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AMQP 0-9-1](https://img.shields.io/badge/Broker-LavinMQ-orange.svg)](https://lavinmq.com/)
[![Intent Classifier](https://img.shields.io/badge/TypeSafe%20AI-Jev%20System%20One-red.svg)](https://api.typesafe.ai)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> **"One microservice = One business intent"**  
> An exploratory Proof-of-Concept and architectural blueprint investigating intent-first routing and end-to-end traceability across event-driven microservices.

---

## 📖 Architectural Exploration & Scope

This repository provides a concrete reference implementation exploring the hypothesis introduced by [Giuseppe Vincenzi](https://www.linkedin.com/in/giuseppevincenzi/) (IT Process Architect) in the article:  
👉 **[Intent-Driven Architecture: a microservice molecule driven by Business and Events](https://www.linkedin.com/pulse/intent-driven-architecture-microservice-molecule-driven-vincenzi-jzdhe/)**

Rather than claiming an industry-wide standard, this project serves as a **working architectural laboratory** to address a specific distributed systems question:  
*Can we treat natural language business intent as the sole public gateway of a microservice molecule, while keeping the resulting asynchronous workflows observable, measurable, and deterministic in production?*

The architecture bridges **Domain-Driven Design (DDD)** and **Event-Driven Architecture (EDA)** across **three foundational pillars**:

1. **The Asynchronous Intent Distributor (The Spike / Alpha)**: A qualified single entry point that ingests natural language, evaluates business intents with low-latency classification (via TypeSafe AI Jev speculative fan-out), and dispatches discrete events to the broker.
2. **End-to-End Correlation ID**: Every intent lifecycle is minted with a unique identifier that propagates across every downstream queue, worker, and return event.
3. **Observability & Closed-Loop Feedback**: A dedicated read-model service (`Desk-Service`) that tracks intermediate states and emits completion events to close the feedback loop.

### Scope & Nature of this Project
This project is an **architectural prototype and didactic blueprint**, not a turnkey enterprise package. Its purpose is to demonstrate structural patterns—intent-based fan-out, Correlation ID propagation, CQRS-style read-models, and reactive deferred completion—with minimal accidental complexity. Enterprise concerns such as distributed tracing (OpenTelemetry), API security, and Dead Letter Exchanges are discussed as architectural extension points.

---

## 🏛️ The Architectural Triad: Three Core Interaction Patterns

Rather than exposing fragmented, resource-oriented REST endpoints (`/orders`, `/status`, `/wait`), the entire molecule exposes a **single universal entry point (`POST /intent`)**. 

Depending on the classified intent, the gateway dynamically activates one of the **three foundational patterns of modern distributed systems**:

```
                       POST /intent
                            │
               ┌────────────┴────────────┐
               │  TypeSafe AI Jev Router │
               └────────────┬────────────┘
                            │
       ┌────────────────────┼────────────────────┐
       ▼                    ▼                    ▼
[ 1. Write-Path ]    [ 2. Read-Path ]     [ 3. Reactive-Path ]
  Async Fire & Forget  Snapshot Point-in-Time Long-Poll Event Stream
  (ORDER_DRINK / FOOD) (CHECK_STATUS)         (AWAIT_READY)
  ↳ 202 Accepted       ↳ 200 OK (State Snapshot) ↳ 200 OK (On Completion)
```

1. **Asynchronous Command / Write-Path (`ORDER_DRINK`, `ORDER_FOOD`)**:
   - Dispatches work to the responsible worker microservices (`Counter` or `Kitchen`).
   - Non-blocking: returns `202 Accepted` immediately with the minted `correlationId`.
   - Supports **Speculative Fan-out**: compound requests like *"I'd like a cappuccino and a croissant"* emit multiple events in parallel under the same `correlationId`.
2. **Point-in-Time Snapshot Query / Read-Path (`CHECK_STATUS`)**:
   - Queries the aggregated materialized view in the `Desk-Service`.
   - Returns an immediate snapshot of the order's internal progression (e.g. `cappuccino: READY, croissant: ORDERED -> IN_PROGRESS`).
3. **Reactive Long-Poll / Completion Notification (`AWAIT_READY`)**:
   - Suspends the incoming HTTP connection in a non-blocking fashion (`DeferredResult`).
   - Unblocks reactively when the `Desk-Service` evaluates that *all* items have completed and emits `OrderReadyEvent`.

---

## 🔍 Pure Observability: Why Both `CHECK_STATUS` and `AWAIT_READY` Matter

A common architectural trap in distributed systems is conflating **state inspection** with **completion notification**. This project intentionally decouples them to achieve **Pure Observability**:

| Dimension | `CHECK_STATUS` (Snapshot Inspection) | `AWAIT_READY` (State-Transition Push) |
|---|---|---|
| **Nature** | **Synchronous / Instantaneous** (~5ms) | **Reactive / Suspended** (1s to 30s) |
| **Semantic Question** | *"What is happening inside the molecule right now?"* | *"Notify me the exact instant everything is done."* |
| **Domain State** | Inspects intermediate states (`IN_PROGRESS`, item-by-item breakdown) | Awaits terminal readiness (`READY`) |
| **Resilience & Diagnostics** | Essential when an await times out, allowing operators or users to diagnose partial failures or bottlenecks. | Provides smooth, zero-polling client UX without burning CPU or network bandwidth. |

By supporting both through natural language, the architecture ensures that the system is **transparent and inspectable at every stage of the lifecycle**, directly addressing the question of observability and controllability in distributed environments.

---

## ☕ The Domain: Virtual Bar

The architecture models a **Virtual Bar** with a single universal entry point and specialized workers:

![IDEA - Virtual Bar Architecture Flow](src/main/resources/static/images/schema.png)

### The Universal Entry Point (`POST /intent`)

Clients interact exclusively through `POST /intent`. Jev classifies the request into one or more business intents via speculative fan-out:

| Business Intent (`IntentEnum`) | Target Worker | LavinMQ Routing Key | Behavior |
|---|---|---|---|
| **`ORDER_DRINK`** | `Counter-Service` | `intent.orderDrink` | Asynchronous (`202 Accepted`) |
| **`ORDER_FOOD`** | `Kitchen-Service` | `intent.orderFood` | Asynchronous (`202 Accepted`) |
| **`CHECK_STATUS`** | `Desk-Service` | `intent.checkStatus` | Synchronous snapshot via Correlation ID |
| **`AWAIT_READY`** | `Desk-Service` | `event.orderReady` (listener) | Reactive long-poll via `DeferredResult` |

---

## 📡 AMQP Topology & Message Flow (LavinMQ)

All inter-service communication flows through a single **Topic Exchange** named **`bar.exchange`**. Microservices never call each other directly; they only bind their dedicated queues using specific routing keys or wildcard patterns.

```
                           ┌────────────────────────────────────────┐
                           │       TOPIC EXCHANGE: bar.exchange     │
                           └─┬──────────────┬──────────────┬──────┬─┘
                             │              │              │      │
          intent.orderDrink  │              │              │      │ event.orderReady
         ────────────────────┘              │              │      └────────────────────┐
        │                 intent.orderFood  │              │ intent.checkStatus        │
        │                ───────────────────┘              │                           │
        │               │                     event.*Ready │                           │
        │               │               intent.order*      │                           │
        ▼               ▼              ────────────────────┘                           ▼
┌──────────────┐ ┌──────────────┐    ┌───────────────────────────┐         ┌───────────────────────────┐
│q.counter.    │ │ q.kitchen.   │    │      q.desk.events        │         │   q.dispatcher.responses  │
│   drinks     │ │    food      │    │ (Order Lifecycle Tracker) │         │ (Unblocks Waiting HTTP)   │
└───────┬──────┘ └──────┬───────┘    └─────────────┬─────────────┘         └─────────────┬─────────────┘
        │               │                          │                                     │
   Counter-Service Kitchen-Service                 │                                     │
        │               │                          ▼                                     ▼
        │ drinkReady    │ foodReady        Desk-Service (Read Model)               Bar-Dispatcher
         ───────────────┴────────────────────────► │ (OrderReadyEvent / StatusReport)   (The Spike)
                                                   └─────────────────────────────────────┘
```

### Complete Routing & Binding Matrix

| Publisher | Routing Key | Target Queue | Consumer | Semantic Payload / Event |
|---|---|---|---|---|
| **Dispatcher** | `intent.orderDrink` | `q.counter.drinks` | `bar-counter` | `OrderDrinkIntentEvent` (item text, correlationId) |
| **Dispatcher** | `intent.orderFood` | `q.kitchen.food` | `bar-kitchen` | `OrderFoodIntentEvent` (item text, correlationId) |
| **Dispatcher** | `intent.order*` *(wildcard)* | `q.desk.events` | `bar-desk` | Registers expected items as `ORDERED` in the lifecycle tracker |
| **Counter** | `event.drinkReady` | `q.desk.events` | `bar-desk` | `DrinkReadyEvent` (marks drink `READY`) |
| **Kitchen** | `event.foodReady` | `q.desk.events` | `bar-desk` | `FoodReadyEvent` (marks food `READY`) |
| **Dispatcher** | `intent.checkStatus` | `q.desk.queries` | `bar-desk` | `CheckStatusIntentEvent` (correlationId) |
| **Desk** | `event.orderStatusReported` | `q.dispatcher.responses` | `bar-dispatcher` | `OrderStatusReportedEvent` (aggregates item states) |
| **Desk** | `event.orderReady` | `q.dispatcher.responses` | `bar-dispatcher` | `OrderReadyEvent` (unblocks pending `AWAIT_READY` requests) |

---

## 🧩 Molecule Structure (Maven Modules)

The repository is organized as a multi-module Maven project (`com.gist:idea-virtual-bar:1.0.0`):

| Module | Architectural Role | Description |
|---|---|---|
| **`bar-common`** | **Contracts & Kernel** | Shared immutable Java records for domain events (`DomainEvent`), `IntentEnum`, and AMQP topology definitions (`AmqpTopology`). |
| **`bar-dispatcher`** | **The Spike (Alpha)** | The sole public entry point (`POST /intent`). Classifies intents via **TypeSafe AI Jev**, assigns `correlationId`, and publishes to LavinMQ. |
| **`bar-counter`** | **Drink Worker** | Consumes `intent.orderDrink`, simulates preparation, and publishes `drinkReady`. |
| **`bar-kitchen`** | **Food Worker** | Consumes `intent.orderFood`, simulates preparation, and publishes `foodReady`. |
| **`bar-desk`** | **Read Model & Aggregator**| Tracks item states, answers status queries, and publishes completion events when orders are `READY`. |

---

## 🧠 Intent Classification with TypeSafe AI Jev

Rather than using a slow generative LLM, the Dispatcher integrates **TypeSafe AI's Jev**—a specialized System One decision model:

- **Ultra-Low Latency (70–200ms)**: Fast, deterministic classification without free-text generation.
- **Speculative Fan-Out**: Evaluates multiple choice questions (`order_drink`, `order_food`, `check_status`, `await_ready`) concurrently in a single HTTP request.
- **Confidence Safety Gating**: Rejects ambiguous inputs below the confidence threshold (`0.65`) before publishing any events.
- **Zero Third-Party SDK Bloat**: Integrated cleanly via Spring Boot's native `RestClient` and Java records.

---

## 🛠️ Technology Stack

- **Language**: Java 21 LTS
- **Framework**: Spring Boot 3.5.5
- **Message Broker**: [LavinMQ](https://lavinmq.com/) (AMQP 0-9-1 cloud instance or local)
- **Intent Classifier**: [TypeSafe AI Jev (System One Model)](https://api.typesafe.ai)
- **HTTP Client**: Spring Boot `RestClient`
- **Async Web**: Spring MVC `DeferredResult`
- **Build Tool**: Maven

---

## 🚀 Prerequisites & Quickstart

### 1. Prerequisites
- **JDK 21 LTS** installed and configured (`java -version`).
- Maven 3.9+.
- An accessible **LavinMQ** instance (e.g. CloudAMQP free tier or local container).
- A **TypeSafe AI** API key for Jev.

### 2. Clone the repository
```bash
git clone https://github.com/gvincenzi/idea-virtual-bar.git
cd idea-virtual-bar
```

### 3. Configure environment variables
```bash
export LAVINMQ_URL="amqps://username:password@instance.lavinmq.com/vhost"
export JEV_API_KEY="your-typesafe-jev-api-key"
```

### 4. Build the project
```bash
mvn clean install
```

---

## 👤 Author

**Giuseppe Vincenzi**
- LinkedIn: [@giuseppevincenzi](https://www.linkedin.com/in/giuseppevincenzi/)
- Reference article: [Intent-Driven Architecture: a microservice molecule driven by Business and Events](https://www.linkedin.com/pulse/intent-driven-architecture-microservice-molecule-driven-vincenzi-jzdhe/)

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.