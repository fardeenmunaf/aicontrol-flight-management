# Project AIControl

## 1. Description of the Project

Project AIControl is a prototype software solution designed for flight management and air traffic control simulation. The system aims to manage complex aviation operations, validate flight plans, and simulate air traffic to ensure safety and prevent mid-air collisions.

This integrated system is composed of several core modules:
* **Backoffice Configuration:** Management of aviation assets, including aircraft models, engine models, airports, air transport companies, and flight control areas.
* **Flight Management & DSL:** Creation, parsing, and semantic validation of flight plans using a custom Domain Specific Language (Core Flight DSL) processed via ANTLR.
* **Air Traffic Simulation:** A highly scalable, multi-process, and multi-threaded simulation engine developed in C. It tracks aircraft trajectories, applies aerodynamic physics (Lift and Drag), and detects safety violations in real-time.
* **Weather Integration:** A service that incorporates environmental data (such as wind speed and direction) into the flight simulations to model realistic conditions.
* **Network & Remote Access:** TCP/UDP-based remote access and logging services, including a real-time web visualization dashboard updated dynamically.

## 2. Planning and Technical Documentation

[Planning and Technical Documentation](docs/readme.md)

## 3. How to Build

*To Do*

## 4. How to Execute Tests

*To Do*

## 5. How to Run

*To Do*

## 6. How to Install/Deploy into Another Machine (or Virtual Machine)

*To Do*

## 7. How to Generate PlantUML Diagrams

To generate plantuml diagrams for documentation execute the script (for the moment, only for linux/unix/macos):

    ./generate-plantuml-diagrams.sh


