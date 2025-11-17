# Model-Based Flappy Bird – HCI Difficulty Optimization 

This project is a **Flappy Bird–style game implemented in Java Swing** and extended for a **Human–Computer Interaction (HCI) experiment**.

Instead of being “just a game”, it is designed to:

- Systematically vary game **difficulty parameters**  
- Measure each player’s **survival time**  
- Log the data to a CSV file  
- Use the data later for **modeling optimal difficulty** (e.g., linear regression)

> Course: 휴먼인터페이스미디어  
> Team 12 – Model-based Flappy Bird Game Difficulty Optimization  
> Members: 20211877, 20233351, 20233523, 20236003

---

##Technologies

- **Language**: Java  
- **GUI Library**: Swing  
- **IDE**: Any (IntelliJ, VS Code, Eclipse, etc.)  
- **OS**: Tested on macOS, but should work on any system with JDK 8+

---

## Experiment Overview

We treat Flappy Bird as an HCI testbed.  
The goal is to **optimize game difficulty** based on three independent variables:

### Independent Variables (Game Parameters)

1. **Jump Power (J)** – how strong the bird jumps when SPACE is pressed  
   - Levels: `j0`, `j1`, `j2` → e.g. 8.0, 10.0, 12.0

2. **Pipe Distance (G)** – horizontal distance between pipes  
   - Levels: `g0`, `g1` → e.g. 220, 260 pixels

3. **Hole Size (H)** – vertical hole size between top and bottom pipes  
   - Levels: `h0`, `h1` → e.g. 160, 200 pixels

This yields **3 × 2 × 2 = 12 difficulty conditions**:

`{j0,g0,h0}, {j0,g0,h1}, …, {j2,g1,h1}`

Each condition is encoded as a string like:

```text
j0_g0_h0, j0_g0_h1, ..., j2_g1_h1

