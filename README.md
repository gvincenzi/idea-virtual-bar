# ☕ IDEA - Intent Driven Event Architecture (Virtual Bar)

[![Java 25](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.org/)
[![Spring Boot 3.5.5](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AMQP 0-9-1](https://img.shields.io/badge/Broker-LavinMQ-orange.svg)](https://lavinmq.com/)
[![Intent Classifier](https://img.shields.io/badge/TypeSafe%20AI-Choice%20Primitive-red.svg)](https://spring-ai-community.github.io/spring-ai-typesafe/latest/concepts/primitives/#choice)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> **"One microservice = One business intent"**  
> Reference implementation project demonstrating the **Intent-Driven Event Architecture (IDEA)** pattern.

---

## 📖 Architectural Background & Vision

This project serves as a hands-on didactic implementation of the principles introduced by [Giuseppe Vincenzi](https://www.linkedin.com/in/giuseppevincenzi/) in the article:  
👉 **[L’Architecture Intent-Driven : une molécule de microservices pilotée par le métier et les événements](https://www.linkedin.com/pulse/intent-driven-architecture-microservice-molecule-driven-vincenzi-jzdhe/)**

The **Intent-Driven Architecture** paradigm posits business intent as the primary entry point of a distributed software system, blending the core strengths of **Domain-Driven Design (DDD)** and **Event-Driven Architecture (EDA)** across **three foundational pillars**:

1. **The Asynchronous Intent Distributor (Spike / Alpha)**: A qualified single access gateway that ingests natural language requests, evaluates them via a type-safe classification engine (**TypeSafe Spring AI `Choice` primitive**), and dispatches discrete events to the message broker.
2. **End-to-End Correlation ID**: Every incoming intent is minted with a unique correlation identifier that propagates across every downstream microservice and event payload.
3. **Observability & Closed-Loop Feedback**: An aggregated read-model that listens to return events, providing continuous auditability and state verification for any given intent lifecycle.

---

## ☕ The Domain: Virtual Bar

To keep the pattern intuitive and relatable, the architecture models a **Virtual Bar**, where responsibilities map cleanly to distinct business intents.

![IDEA - Virtual Bar Architecture Flow](src/main/resources/static/images/schema.png)

---

## 🧩 Molecule Structure (Maven Modules)

The repository is organized as a multi-module Maven project (`com.gist:idea-virtual-bar:1.0.0`):

| Module | Architectural Role | Description |
|---|---|---|
| **`bar-common`** | **Contracts & Kernel** | Shared immutable Java records for domain events (`DomainEvent`) and AMQP definitions. |
| **`bar-dispatcher`** | **The Spike (Alpha)** | The sole public entry point. Classifies intents via **TypeSafe Spring AI (`Choice`)**, assigns `correlationId`, and publishes to LavinMQ. |
| **`bar-counter`** | **Drink Worker** | Consumes `intent.orderDrink`, simulates preparation, and publishes `drinkReady`. |
| **`bar-kitchen`** | **Food Worker** | Consumes `intent.orderFood`, simulates preparation, and publishes `foodReady`. |
| **`bar-desk`** | **Read Model & Cashier** | Aggregates item states, handles `intent.checkStatus`, executes `intent.payBill`, and issues receipts. |

---

## 🧠 Intent Classification with TypeSafe AI

The **Bar-Dispatcher** replaces brittle regex matching with the **TypeSafe Spring AI `Choice` primitive**:

- **Semantic Disambiguation**: Natural language inputs in any language are mapped to discrete business intents (`order_drink`, `order_food`, `check_status`, `pay_bill`).
- **Confidence Scoring**: Every classification returns a confidence score (`confidence()`) and plausible alternatives (`optionsAbove(threshold)`), enabling policy-driven gating before event emission.
- **Fail-Safe Routing**: Requests with low confidence or classified as `unknown` are rejected at the edge with immediate feedback.

---

## 🛠️ Technology Stack

- **Language**: Java 25
- **Framework**: Spring Boot 3.5.5
- **Message Broker**: [LavinMQ](https://lavinmq.com/) (AMQP 0-9-1 cloud instance or local)
- **Intent Classifier**: [Spring AI Community TypeSafe (`Choice` primitive)](https://spring-ai-community.github.io/spring-ai-typesafe/latest/concepts/primitives/#choice)
- **Build Tool**: Maven

---

## 🚀 Prerequisites & Quickstart

### 1. Prerequisites
- **JDK 25** installed and configured (`java -version`).
- Maven 3.9+.
- An accessible **LavinMQ** instance (e.g. CloudAMQP free tier or local).
- An OpenAI-compatible API key (or local Ollama instance) for the intent classifier.

### 2. Clone the repository
```bash
git clone https://github.com/gvincenzi/idea-virtual-bar.git
cd idea-virtual-bar
```

### 3. Configure environment variables
```bash
export LAVINMQ_URL="amqps://username:password@instance.lavinmq.com/vhost"
export OPENAI_API_KEY="your-api-key"
```

### 4. Build the project
```bash
mvn clean install
```

---

## 👤 Author

**Giuseppe Vincenzi**
- LinkedIn: [@giuseppevincenzi](https://www.linkedin.com/in/giuseppevincenzi/)
- Reference article: [Intent-Driven Architecture on LinkedIn Pulse](https://www.linkedin.com/pulse/intent-driven-architecture-microservice-molecule-driven-vincenzi-jzdhe/)

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.