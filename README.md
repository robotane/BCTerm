# BCTerm

A formal Java bytecode analyzer that models the JVM execution environment for static analysis. BCTerm simulates how bytecode instructions affect the stack, local variables, and heap without actually running the program, enabling bytecode semantics analysis and termination proof validation.

## Features

- Basic JVM state modeling (stack, locals, heap)
- Object allocation and reference tracking
- Support for basic bytecode instructions
- Basic program structure representation with CFG (Control Flow Graph)
- Support for analyses:
  - Pair-sharing analysis
  - Memory graph generation

## Getting Started

### Prerequisites

- Java 8 or higher
- Maven for building the project

### Building the Project

```bash
mvn clean install
```

### Usage

Not yet implemented.

```bash
java -jar bcterm.jar <path-to-class-file>
```

## Project Structure

- `src/main/java/fr/univreunion/bcterm/jvm/state/` - Core JVM state representation (Integer, Location, Null values, Objects)
- `src/main/java/fr/univreunion/bcterm/jvm/instruction/` - Supported bytecode instructions:
  - Stack Operations (Load, Store, Dup, Const)
  - Arithmetic Operations (Add)
  - Object Operations (New, GetField, PutField)
  - Control Flow (IfEqOfType, IfNeOfType)
  - Method Operations (Call)
- `src/main/java/fr/univreunion/bcterm/program/` - Program structure representation (BasicBlock, CFG, Method, Program)
- `src/main/java/fr/univreunion/bcterm/analysis/` - Analysis implementations:
  - `sharing/` - Pair-sharing analysis
  - `graph/` - Memory graph generation

## Contributors

- BAYE Serge Olivier (also known as [John ROBOTANE](https://github.com/robotane))
- Université de La Réunion
