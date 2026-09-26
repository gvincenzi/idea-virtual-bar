# ☕ IDEA - Intent Driven Event Architecture (Virtual Bar)
<img src="src/main/resources/static/images/logo.jpg" width="200">

[![Java 25](https://img.shields.io/badge/Java-25-blue.svg)](https://openjdk.org/)
[![Spring Boot 3.5.5](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AMQP 0-9-1](https://img.shields.io/badge/Broker-LavinMQ-orange.svg)](https://lavinmq.com/)
[![Intent Classifier](https://img.shields.io/badge/TypeSafe%20AI-Jev%20System%20One-red.svg)](https://api.typesafe.ai)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> **"One microservice = One business intent"**  
> Reference implementation demonstrating the **Intent-Driven Event Architecture (IDEA)** pattern.

---

## 📖 Architectural Background & Vision

This project is a hands-on implementation of the architectural principles formulated by [Giuseppe Vincenzi](https://www.linkedin.com/in/giuseppevincenzi/) in:  
👉 **[L’Architecture Intent-Driven : une molécule de microservices pilotée par le métier et les événements](https://www.linkedin.com/pulse/intent-driven-architecture-microservice-molecule-driven-vincenzi-jzdhe/)**

The **Intent-Driven Architecture** treats business intent as the primary entry point of a distributed software system, harmonizing **Domain-Driven Design (DDD)** and **Event-Driven Architecture (EDA)** across **three foundational pillars**:

1. **The Asynchronous Intent Distributor (Spike / Alpha)**: A qualified single access gateway that ingests free-form natural language, classifies intents at ultra-low latency using **TypeSafe AI Jev (System One Model)** with speculative fan-out, and dispatches discrete events to the message broker.
2. **End-to-End Correlation ID**: Every incoming intent lifecycle receives a unique correlation identifier that travels through every worker, topic, and return event.
3. **Observability & Closed-Loop Feedback**: A dedicated read-model service (`Desk-Service`) that aggregates state across workers, providing continuous auditability and state verification.

---

## ☕ The Domain: Virtual Bar

The architecture models a **Virtual Bar** with a single universal entry point and specialized workers:

![IDEA - Virtual Bar Architecture Flow](src/main/resources/static/images/schema.png)

### The Universal Entry Point (`POST /intent`)

Clients interact exclusively through a single endpoint. Jev classifies the request into one or more business intents via speculative fan-out:

| Business Intent (`IntentEnum`) | Target Worker | LavinMQ Routing Key | Behavior |
|---|---|---|---|
| **`ORDER_DRINK`** | `Counter-Service` | `intent.orderDrink` | Asynchronous (`202 Accepted`) |
| **`ORDER_FOOD`** | `Kitchen-Service` | `intent.orderFood` | Asynchronous (`202 Accepted`) |
| **`CHECK_STATUS`** | `Desk-Service` | `intent.checkStatus` | Synchronous bridge via Correlation ID |
| **`PAY_BILL`** | `Desk-Service` | `intent.payBill` | Synchronous bridge via Correlation ID |

> **Compound Intents Supported**: A sentence like *"I'd like a cappuccino and a croissant"* triggers both `ORDER_DRINK` and `ORDER_FOOD` in parallel under a **single Correlation ID**, dispatched across separate queues.

---

## 🧩 Molecule Structure (Maven Modules)

The repository is organized as a multi-module Maven project (`com.gist:idea-virtual-bar:1.0.0`):

| Module | Architectural Role | Description |
|---|---|---|
| **`bar-common`** | **Contracts & Kernel** | Shared immutable Java records for domain events (`DomainEvent`), `IntentEnum`, and AMQP topology definitions. |
| **`bar-dispatcher`** | **The Spike (Alpha)** | The sole public entry point (`POST /intent`). Classifies intents via **TypeSafe AI Jev**, assigns `correlationId`, and publishes to LavinMQ. |
| **`bar-counter`** | **Drink Worker** | Consumes `intent.orderDrink`, simulates preparation, and publishes `drinkReady`. |
| **`bar-kitchen`** | **Food Worker** | Consumes `intent.orderFood`, simulates preparation, and publishes `foodReady`. |
| **`bar-desk`** | **Read Model & Cashier** | Aggregates item states, handles `intent.checkStatus`, executes `intent.payBill`, and issues receipts. |

---

## 🧠 Intent Classification with TypeSafe AI Jev

Rather than using a slow generative LLM, the Dispatcher integrates **TypeSafe AI's Jev**—a specialized System One decision model:

- **Ultra-Low Latency (70–200ms)**: Fast, deterministic classification without free-text generation.
- **Speculative Fan-Out**: Evaluates multiple choice questions (`order_drink`, `order_food`, `check_status`, `pay_bill`) concurrently in a single HTTP request.
- **Confidence Safety Gating**: Rejects ambiguous inputs below the confidence threshold (`0.65`) before publishing any events.
- **Zero Third-Party SDK Bloat**: Integrated cleanly via Spring Boot's native `RestClient` and Java records.

---

## 🛠️ Technology Stack

- **Language**: Java 25
- **Framework**: Spring Boot 3.5.5
- **Message Broker**: [LavinMQ](https://lavinmq.com/) (AMQP 0-9-1 cloud instance or local)
- **Intent Classifier**: [TypeSafe AI Jev (System One Model)](https://api.typesafe.ai)
- **HTTP Client**: Spring Boot `RestClient`
- **Build Tool**: Maven

---

## 🚀 Prerequisites & Quickstart

### 1. Prerequisites
- **JDK 25** installed and configured (`java -version`).
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
- Reference article: [Intent-Driven Architecture on LinkedIn Pulse](https://www.linkedin.com/pulse/intent-driven-architecture-microservice-molecule-driven-vincenzi-jzdhe/)

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.