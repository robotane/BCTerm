
# BCTerm

A formal Java bytecode interpreter that models the JVM execution environment with precise tracking of states during bytecode execution. Designed for program verification, bytecode semantics analysis, and termination proof validation.

## Overview

BCTerm implements a formal model of the Java Virtual Machine state, representing the complete execution environment including local variables, operand stack, and memory heap. This allows for precise tracking and analysis of bytecode execution.

## Features

- Complete JVM state modeling
- Detailed state visualization
- Object allocation and reference tracking
- Bytecode execution simulation

## Getting Started

### Prerequisites

- Java 8 or higher
- Maven for building the project

### Building the Project

```bash
mvn clean install
```

### Running BCTerm

```bash
java -jar bcterm.jar <path-to-class-file>
```

## Project Structure

- `src/main/java/fr/univreunion/bcterm/jvm/state/` - Core JVM state representation
- `JVMState.java` - Implementation of the formal JVM state model
- Additional packages for bytecode parsing, execution, and analysis

## Contributors
- BAYE Serge Olivier (Alias [John ROBOTANE](https://github.com/robotane))
- Université de La Réunion
