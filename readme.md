# Perspective Highway

A keyboard-driven Java mini game that mixes a faux-3D look with classic 2D lane-dodging. Slide your car between three highway lanes, avoid oncoming traffic, and push your score higher as the game speeds up.

The refreshed build amps up the perspective illusion with easing lane changes, tapered road stripes, and a scenic dusk skyline that moves beneath you as the action accelerates.

## Gameplay Features

- **Three-lane highway:** Use the left and right arrow keys to dodge randomly spawning cars.
- **Pseudo-3D presentation:** A perspective-styled road, tapered lane stripes, and car scaling create a 3D feel while using simple 2D drawing.
- **Smooth lane shifts:** Player movement eases toward the target lane for a more natural, arcade-style glide.
- **Dynamic difficulty:** The game gradually increases speed and spawn frequency as your score climbs.
- **Pause and resume:** Press `P` at any time to pause the action. Press `Enter` after a crash to restart.

## Controls

| Key | Action |
| --- | --- |
| ← / → | Shift the player car between the three lanes. |
| P | Toggle pause/resume. |
| Enter | Restart after a collision. |

## Building and Running

The game uses standard Java Swing. Any Java 11+ JDK will work.

```bash
# Compile
javac -d out $(find src -name "*.java")

# Run
java -cp out com.perspectiveracer.GameApp
```

Running the game opens a window sized 480×640. Keep dodging to increase your score and level; every 10 passed cars ramps up the challenge. The horizon backdrop and depth-scaled opponents highlight the faux-3D sensation as the speed climbs.

## Project Layout

```
src/
└── main/java/com/perspectiveracer/
    ├── GameApp.java      # Launches the Swing window
    └── GamePanel.java    # Game loop, rendering, and input handling
```
