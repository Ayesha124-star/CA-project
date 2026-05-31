# Pipeline Hazard Visualizer (PHV)

A real-time MARS plugin for interactive MIPS pipeline simulation, hazard detection, CPI analysis, and timing diagram visualization.

## Overview

Pipeline Hazard Visualizer (PHV) is an educational extension for the MARS (MIPS Assembler and Runtime Simulator) environment that helps students visualize how instructions move through a classic 5-stage MIPS pipeline.

The tool simulates:

* Instruction Fetch (IF)
* Instruction Decode (ID)
* Execute (EX)
* Memory Access (MEM)
* Write Back (WB)

PHV detects RAW (Read-After-Write) data hazards, supports optional forwarding, inserts stalls when necessary, and generates a live timing diagram updated cycle-by-cycle.

This project was developed as part of the Computer Architecture course.

---

## Features

### Real-Time Pipeline Visualization

* Interactive timing diagram
* Cycle-by-cycle pipeline progression
* Dynamic stage updates

### Hazard Detection

* RAW hazard detection
* Load-use hazard identification
* Automatic stall insertion
* Hazard classification

### Forwarding Support

* Toggle forwarding ON/OFF
* Detect forwarding opportunities
* Compare forwarded vs non-forwarded execution

### Performance Metrics

* Live CPI calculation
* Ideal CPI vs Actual CPI comparison
* Stall statistics
* Instruction categorization

### CSV Export

Export:

* Full timing diagram
* Pipeline stages per cycle
* CPI metrics
* Hazard statistics

Compatible with:

* Microsoft Excel
* LibreOffice Calc
* Python/pandas

### Theme System

Three built-in visual themes:

* Light Theme
* Dark Theme (Catppuccin Mocha inspired)
* High Contrast Accessibility Theme

---

# System Architecture

PHV integrates directly with the MARS simulator through the MARS Tool API.

## Core Components

### 1. Instruction Interceptor

Observes live instruction fetches from the MIPS text segment.

### 2. Pipeline Simulator

Simulates the classic 5-stage in-order MIPS pipeline.

### 3. Hazard Detection Engine

Detects RAW dependencies between in-flight instructions.

### 4. Forwarding Unit

Determines whether hazards can be resolved using bypass paths.

### 5. Timing Diagram Renderer

Renders the interactive JTable-based pipeline visualization.

### 6. CPI Analyzer

Tracks:

* Total cycles
* Stall cycles
* Completed instructions
* Actual CPI

### 7. CSV Export Engine

Exports the full pipeline state and performance metrics.

---

# Hazard Detection Logic

PHV currently supports:

| Hazard Type            | Supported |
| ---------------------- | --------- |
| RAW (Read After Write) | Yes       |
| Load-Use Hazard        | Yes       |
| Forwarding Detection   | Yes       |
| Structural Hazards     | Planned   |
| Control Hazards        | Planned   |

---

# Example Pipeline Execution

```assembly
lw   $t0, 0($s0)
add  $t1, $t0, $t2
sub  $t3, $t1, $t4
```

PHV detects:

* Load-use hazard between `lw` and `add`
* RAW dependency between `add` and `sub`
* Required stall cycles
* Forwarding opportunities

---

# Technologies Used

* Java
* Swing GUI
* MARS Simulator API
* JTable Rendering
* CSV File Export

---

# Installation

## Prerequisites

* Java JDK 8+
* MARS MIPS Simulator

## Steps

1. Clone the repository

```bash
git clone https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
```

2. Open the project in your IDE

Recommended:

* IntelliJ IDEA
* Eclipse
* NetBeans

3. Build the project

Compile the Java source files and generate the `.jar` plugin.

4. Add plugin to MARS

Place the generated JAR inside the MARS tools/plugins directory.

5. Launch MARS

Open:

```text
Tools → Pipeline Hazard Visualizer
```

---

# Project Structure

```text
PHV/
│
├── src/
│   ├── PipelineHazardVisualizer.java
│   ├── PipelineCellRenderer.java
│   ├── HazardDetector.java
│   ├── CPIAnalyzer.java
│   └── CSVExporter.java
│
├── docs/
├── README.md
└── LICENSE
```

---

# Evaluation Results

PHV was tested using representative MIPS benchmark sequences.

## Results

### Independent Instructions

* No hazards detected
* CPI = 1.00

### ALU Dependencies

* Hazards resolved via forwarding
* No stalls introduced

### Load-Use Dependencies

* Correct stall insertion
* Accurate CPI increase

### Mixed Pipeline Workloads

* Correct hazard classification
* Accurate timing diagram rendering

---

# Educational Impact

PHV was designed specifically for Computer Architecture education.

It helps students:

* Understand pipelining visually
* Learn forwarding concepts
* Observe stall insertion
* Analyze CPI effects
* Compare ideal vs real execution

---

# Future Improvements

Planned enhancements include:

* Control hazard detection
* Branch prediction visualization
* Structural hazard simulation
* Cache miss modeling
* Memory hierarchy integration
* Virtualized rendering for large programs
* Multi-cycle functional units

---

# Research Paper

This repository is based on the research project:

**"Pipeline Hazard Visualizer: A MARS Tool for Interactive MIPS Pipeline Simulation and Hazard Detection"**

Authors:

* Ayesha Qaisar Cheema
* Aliza Kunvar

Department of Computer Science and Engineering

---

# Authors

## Ayesha Qaisar Cheema

* Computer Science Student
* AI/ML and Software Development Enthusiast

## Aliza Kunvar

* Computer Science Student
* Systems and Architecture Enthusiast

---

# License

This project is intended for educational and academic use.

---

# Acknowledgments

Special thanks to:

* MARS Simulator developers
* Computer Architecture faculty
* Patterson & Hennessy for foundational pipeline concepts

---

# Contributing

Contributions, suggestions, and improvements are welcome.

Feel free to:

* Fork the repository
* Open issues
* Submit pull requests

---

# Star the Repository

If you found this project useful, consider giving the repository a star.
