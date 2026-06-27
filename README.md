# Tank Battle

A classic tank battle game built with Java Swing. Protect your base and destroy all enemy tanks!

## Screenshot

The game features a 15×15 grid map. The player tank (green) starts at the bottom, while enemy tanks (red) spawn from three points at the top.

## Gameplay

- **Objective**: Destroy all 12 enemy tanks while protecting your base (E)
- **Lives**: The player has 3 lives; respawns at the bottom center after being hit
- **Lose condition**: Lives depleted or base destroyed
- **Win condition**: All enemy tanks destroyed

## Controls

| Key | Action |
|-----|--------|
| W A S D / Arrow keys | Move |
| Space | Fire |
| P | Pause / Resume |
| R | Restart |
| ESC | Quit |

## Running

### Option 1: Compile and run directly

```bash
javac -encoding UTF-8 -d out src/*.java
java -cp out TankBattle
```

### Option 2: Build a JAR and run

Windows CMD:
```bash
build.bat
run.bat
```

Windows PowerShell:
```powershell
.\build.ps1
java -jar TankBattle.jar
```

### Option 3: Run in IDE

Run `src/Main.java` directly in IntelliJ IDEA or any other IDE.

## Project Structure

```
├── src/
│   ├── Main.java          # Debug entry point
│   └── TankBattle.java    # Main game class (all logic and rendering)
├── Manifest.txt            # JAR manifest
├── build.bat               # Windows build script (CMD)
├── build.ps1               # Windows build script (PowerShell)
├── run.bat                 # Run JAR script
└── .gitignore
```

## Tech Stack

- Java (Swing / AWT)
- No third-party dependencies, pure standard library
