# BCTerm

A formal Java bytecode interpreter that models the JVM execution environment with precise tracking of states during bytecode execution. Designed for program verification, bytecode semantics analysis, and termination proof validation.

## Overview

BCTerm implements a formal model of the Java Virtual Machine state, representing the complete execution environment including local variables, operand stack, and memory heap. This allows for precise tracking and analysis of bytecode execution.

## Features

- Complete JVM state modeling
- Detailed state visualization
- Object allocation and reference tracking
- Bytecode execution simulation
- Support for advanced program analyses:
  - Pair-sharing analysis
  - Cyclicity analysis
  - Aliasing analysis
- Path-length analysis for termination verification

## Getting Started

### Prerequisites

- Java 8 or higher
- Maven for building the project

### Building the Project

```bash
mvn clean install
```

### Running BCTerm

Not yet implemented.

```bash
java -jar bcterm.jar <path-to-class-file>
```

## Project Structure

- `src/main/java/fr/univreunion/bcterm/jvm/state/` - Core JVM state representation
- `src/main/java/fr/univreunion/bcterm/jvm/instruction/ `- Bytecode instruction implementations
- `src/main/java/fr/univreunion/bcterm/program/` - Program structure representation
- Additional packages for bytecode parsing, execution, and analysis (TODO)

## Contributors

- BAYE Serge Olivier (also known as [John ROBOTANE](https://github.com/robotane))
- Université de La Réunion
