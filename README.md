# ☕ IDEA - Intent Driven Event Architecture (Virtual Bar)
<img src="src/main/resources/static/images/logo.jpg" width="200">

[![Java 25](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.org/)
[![Spring Boot 3.5.5](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AMQP 0-9-1](https://img.shields.io/badge/Broker-LavinMQ-orange.svg)](https://lavinmq.com/)
[![Rule Engine](https://img.shields.io/badge/Config-TypeSafe%20HOCON-red.svg)](https://github.com/lightbend/config)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> **"One microservice = One business intent"**  
> Reference implementation project demonstrating the **Intent-Driven Event Architecture (IDEA)** pattern.

---

## 📖 Architectural Background & Vision

This project serves as a hands-on didactic implementation of the principles introduced by [Giuseppe Vincenzi](https://www.linkedin.com/in/giuseppevincenzi/) in the article:  
👉 **[L’Architecture Intent-Driven : une molécule de microservices pilotée par le métier et les événements](https://www.linkedin.com/pulse/intent-driven-architecture-microservice-molecule-driven-vincenzi-jzdhe/)**

The **Intent-Driven Architecture** paradigm posits business intent as the primary entry point of a distributed software system, blending the core strengths of **Domain-Driven Design (DDD)** and **Event-Driven Architecture (EDA)** across **three foundational pillars**:

1. **The Asynchronous Intent Distributor (Spike / Alpha)**: A qualified single access gateway that ingests natural language or abstract requests, parses them via a rule-based engine (**TypeSafe Config / HOCON**), and dispatches discrete events to the message broker.
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
| **`bar-dispatcher`** | **The Spike (Alpha)** | The sole public entry point. Parses intents via **TypeSafe Config**, assigns `correlationId`, and publishes to LavinMQ. |
| **`bar-counter`** | **Drink Worker** | Consumes `intent.orderDrink`, simulates preparation, and publishes `drinkReady`. |
| **`bar-kitchen`** | **Food Worker** | Consumes `intent.orderFood`, simulates preparation, and publishes `foodReady`. |
| **`bar-desk`** | **Read Model & Cashier** | Aggregates item states, handles `intent.checkStatus`, executes `intent.payBill`, and issues receipts. |

---

## 🛠️ Technology Stack

- **Language**: Java 25
- **Framework**: Spring Boot 3.5.5
- **Message Broker**: [LavinMQ](https://lavinmq.com/) (AMQP 0-9-1 cloud instance or local)
- **Rule Engine**: [TypeSafe Config (HOCON)](https://github.com/lightbend/config)
- **Build Tool**: Maven

---

## 🚀 Prerequisites & Quickstart

### 1. Prerequisites
- **JDK 25** installed and configured (`java -version`).
- Maven 3.9+.

### 2. Clone the repository
```bash
git clone https://github.com/your-username/idea-virtual-bar.git
cd idea-virtual-bar
```

### 3. Configure your LavinMQ connection
Export the AMQPS connection URI provided by your cloud LavinMQ instance (or point to local):

```bash
export LAVINMQ_URL="amqps://username:password@instance.lavinmq.com/vhost"
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