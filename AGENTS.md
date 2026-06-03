# AGENTS.md - NullpoMino_Alt Development Guide

## Project Overview

NullpoMino_Alt is a Kotlin/Java stacker game-engine in the same genre as Tetris with sophisticated game modes, AI 
capabilities, and 
modular 
architecture. The codebase is production-grade with ~7.7K version history.

**Tech Stack**: Kotlin 2.4.0 + Java 26, Maven multi-module, Slick2D graphics 1.0.2, LWJGL for OpenGL binding.

---

## Architecture Fundamentals

### Core Game Loop Structure

The game uses a **state machine + frame-based update pattern**:

1. **GameEngine** (`mu.nu.nullpo.game.play.GameEngine`): Core game logic
   - Manages field, pieces, piece queue, statistics
   - Handles piece movement, rotation, line clear calculations
   - Has **statc[]** array: frame counters for state-specific logic (e.g., `statc[0]` tracks frames in current state)

2. **GameManager** (`mu.nu.nullpo.game.play.GameManager`): Container
   - Owns the EventReceiver (rendering), GameMode instance, audio manager
   - Coordinates between game logic and UI

3. **EventReceiver** (`mu.nu.nullpo.game.event.EventReceiver`): Rendering/Events
   - Hook point for custom rendering at different phases
   - Lifecycle hooks: `onFirst()`, `onSetting()`, `onReady()`, `startGame()`, `onLast()`
   - Different receivers for different UI (Slick2D in `mu.nu.nullpo.gui.slick`)

4. **GameMode** (interface, extended by `AbstractMode`): Game mode logic
   - Defines rules, scoring, piece behavior per mode
   - **Key lifecycle**: `onSettingChanged()` → `onReady()` → `onMove()` → `onLineClear()` → `onGameOver()` or `onExcellent()` 
   - Every frame: mode's `onFirst()` and `onLast()` called around engine update

### Game States (Status enum in GameEngine)

```
SETTING → PROFILE → READY → 
  MOVE → LOCKFLASH → LINECLEAR → ARE → MOVE → ...
  (ENDINGSTART → ENDING → EXCELLENT → )GAMEOVER → RESULT
```

**Critical detail**: Frame logic uses `engine.stime` (time in current state). When `stime==0`, it's the first frame of that state—use this for one-time initialization (see `onReady()` implementations in any mode).

---

## Project Structure

```
nullpomino-core/
  src/main/kotlin/mu/nu/nullpo/
    game/
      play/           # GameEngine, GameManager, core loop
      component/      # Field, Piece, Controller, Statistics
      subsystem/mode/ # Game modes (Grand, Retro, VS, etc.)
      event/          # EventReceiver interface, ScoreEvent
      net/            # Networking layer (NetServer, NetClient)
    gui/slick/        # Slick2D UI states, main entry (NullpoMinoSlick.kt)
    tool/             # Editors (RuleEditor, NetAdmin, AIRanksTool)
    util/             # Utilities, config management

nullpomino-run/
  config/             # Game rules, language files, settings
  res/                # Graphics, sounds, fonts (graphics/*, se/*, bgm/*)
  scripts/            # Launch scripts per platform
  pyai-scripts/       # Python AI implementations for testing
```

---

## Essential Developer Workflows

### Build & Run

**Build**: `mvn clean install` or use IDE
- Generates `target/install/` with executable layout
- Bundles JAR, libs, resources via maven-assembly & maven-dependency plugins

**Run Main Game (Windows)**:
```bash
cd nullpomino-run/scripts
play_slick.bat
```

**Key classes in startup** (`NullpoMinoSlick.kt` companion object main):
1. Load config XML files (music, language, rule descriptions)
2. Load ModeManager (discovers all modes from `mode.lst`)
3. Create AppGameContainer + StateBasedGame
4. Initialize all UI states (title, settings, in-game, etc.)

### Add a Custom Game Mode

1. Create class extending `AbstractMode` in `mu.nu.nullpo.game.subsystem.mode`
2. Override key hooks:
   - `onSettingChanged()`: Update speed/rules
   - `onReady()`: Init per-game state (if `stime==0`)
   - `onMove()`: Handle piece movement frame logic
   - `onLineClear()`: Custom score/sound when lines clear
   - `renderMove()`, `renderLast()`: Custom HUD rendering
3. Add to `mode.lst` resource file for discovery

**Example pattern**:
```kotlin
override fun onReady(engine:GameEngine):Boolean {
  if(engine.stime==0) {  // First frame only
    engine.statistics.level = startLevel
    setSpeed(engine)
  }
  return false  // false = use default ready logic
}
```

### Modify Game Rules

**Rules defined in**: `config/rule/*.rul` or `.rul.gz` files + `RuleOptional` class
- Contains: piece spawn height, wallkick algorithm, gravity, ARE (appearance rate), line delay
- Loaded by `RuleConf` on game start
- **Runtime access**: `engine.ruleOpt` (RuleOptional instance)

---

## Code Patterns & Conventions

### Style (from codestyle.xml)
- **Indentation**: 2 spaces (tabs for Java/Kotlin), NO spaces around operators
- **Right margin**: 123 characters
- **Kotlin specifics**: No spaces after `:`, before `:`

### Frame-Based State Pattern
```kotlin
// statc[0] = frame counter; when it reaches threshold, advance state
if(stime<60) {
  // Frame logic
} else {
  engine.stat = Status.NEXT_STATE
  engine.resetStatc()  // Reset stime/statc[0]
}
```

### Mode Rendering Hooks
Order per frame (in GameEngine render phase):
1. `receiver.renderFirst()`
2. Mode-specific render (based on current Status)
3. `receiver.renderLast()` (for HUD overlays)

### Field & Piece System
- **Field**: 10W×20H default (configurable), `engine.field.getBlock(x,y)` for block objects
- **Piece**: 4×4 collision grid, rotation states (0-3), colors 0-6
- **Wallkick**: Algorithm in `wallkick` parameter (e.g., SRS—Super Rotation System)
- **Gravity**: Piece falls 1-cell when `gCount >= speed.denominator` (e.g., 256)

### AI Integration
- **DummyAI/BasicAI**: Base threading pattern in `mu.nu.nullpo.game.subsystem.ai`
- Thread-safe: `thinkRequest` flag set when new piece arrives
- `thinkBestPosition()`: Called per frame or threaded, returns move inputs
- **Python AI**: `pyai-scripts/` for prototyping (integrates via JEP)

---

## Configuration & Data Management

### Properties System (CustomProperties)
- XML-based config files under `config/setting/`, `config/lang/`, `config/rule/`
- Load: `propConfig.load(FileInputStream(...))`
- Access: `propConfig.getProperty("key", defaultValue)`

### Replay System
- Binary as Gzip-compressed property in `mu.nu.nullpo.game.component.Replay`
- Records: seed, rule, inputs per frame, statistics
- Stored in replay folder with JSON metadata

### Statistics Tracking (Statistics class)
- Score, level, lines, time played, combo, B2B
- Custom fields per mode (see mode properties)

---

## Testing & Debugging Tips

### Run Tests
```bash
mvn test  # Runs JUnit 5 tests in src/test/
```

### Enable Debug Output
- Modify `config/etc/log.xml` (log4j2 config)
- Set logger level to DEBUG for `mu.nu.nullpo` packages

### Inspect Game State
- **Runtime**: statc[], stime in frame 0 checks
- **Field dump**: Use `engine.field.getBlock(x,y)` or `field.string` property
- **Piece debug**: `engine.nowPieceX/Y`, `engine.nowPieceObject.id`

### Replay Debugging
- Save replay after game
- Load via StateReplaySelect
- Frame-step with keyboard to inspect input at each frame

---

## Integration Points & External Dependencies

### Key Dependencies
- **Slick2D 1.0.2**: 2D graphics (replaces older AWT)
- **LWJGL 2.9.3**: OpenGL binding + input
- **JInput 2.0.9**: Joystick input abstraction
- **Log4j 2.25.4**: Logging
- **Kotlinx Serialization 1.10.0**: JSON/binary serialization
- **JLine 3.25.1**: Terminal I/O for console tools

### Network Layer
- `NetServer`: Standalone server for multiplayer
- `NetClient`: Connects to server for vs/coop modes
- Protocol: Text-based commands over TCP

### Resource Pack System
- Graphics: `nullpomino-run/res/graphics/{blockskin/normal,effects,frames}/`
- Sounds: `res/{se,bgm,jingle}/` (OGG/WAV)
- Fonts: `res/font/*.ttf` (loaded by RendererSlick)

---

## Common Pitfalls & Gotchas

1. **Frame 0 initialization**: Always check `engine.stime==0` before one-time setup in lifecycle hooks
2. **statc[] vs stime**: `statc[0]` is raw frame counter, `stime` is time in current state (same thing post-resetStatc)
3. **Gravity**: Add to `gCount` each frame; when `gCount >= denominator`, gravity pulls down
4. **Wallkick complexity**: Different modes need different wallkick styles; SRS is default but customizable
5. **AI threading**: If `engine.aiUseThread=true`, AI runs async; sync with frame updates via `thinkRequest` flag
6. **Rendering order**: Mode rendering happens DURING game state processing, not after—beware of frame lag visuals
7. **Kotlin null safety**: Use `?.let{}` and Elvis `?:` extensively; this pattern dominates the codebase

---

## Quick Reference: Key Classes

| Class | Package | Purpose |
|-------|---------|---------|
| GameEngine | play | Game loop, state machine, piece logic |
| GameManager | play | Container, event hub |
| EventReceiver | event | Rendering hooks, UI callbacks |
| AbstractMode | subsystem.mode | Base for all game modes |
| Field | component | 10×20 play area, block storage |
| Piece | component | Tetromino, rotation states |
| Controller | component | Input state (buttons, axis) |
| RendererSlick | gui.slick | Slick2D graphics backend |
| NullpoMinoSlick | gui.slick | Main entry, state machine setup |
| Statistics | component | Score, level, line tracking |
| RuleOptional | component | Rule parameters (gravity, ARE, etc.) |
| Replay | component | Replay record/playback |

---

## Documentation Resources

- **README.md**: Startup commands, resource pack layout
- **config/lang/modedesc_*.xml**: Mode descriptions (human-readable)
- **Source comments**: Extensive Japanese + English comments throughout
- **Bitbucket CI**: bitbucket-pipelines.yml defines build steps

For deep dives, trace execution from `NullpoMinoSlick.main()` → `StateInGame.update()` → `GameEngine.update()`.

