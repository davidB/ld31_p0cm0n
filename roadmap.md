# core
x display regular pellet in correct place (pellet are lights)
x display a pacman (cube) that move via control only on allowed tile (keyboard control / ZQSD/WASD / arrow)
x pacman eat pellet => pellet disappear, countdown pellet
x display timer on hud
x 0 pellet => end screen overlay + retry button
x add speed power (x3) to pacman + juicy ribon/tail
x add wall (in blender ? , generated from the map ??)
x remove some pellets like in the classic pacman
x intro screen : "arrow + eat all pointlight as quick as you can" (press any key to start)
- package native + jar (getdown plugin) + try it on linux and windows

# polish 1
x a sound fx when pacman eat pellet, energizer pellet
- particle animation when boost (+ change light color ?)

# enhance (ghosts)
x add 1 ghost (cube + color) when ghost hit pacman : game over
- add first IA for ghost
- add 2nd ghost + IA
- add 3nd ghost + IA
- add 4nd ghost + IA

# polish 2
x a sound fx when ghost hit pacman
x ghosts are particle emitter like fire ball, smoke when fright
- add joystick support
! try cube moving animation (mini jump ?)
- add jump power(x3) to pacman

# polish 3
- use noise (perlin,...) to generate level of wall
- a start screen overlay
x add voice "Game Over !" record
- volumetric light around pacman

# publish
- disable debug (log, println, stats, (fps ?),...)
- package native + jar (getdown plugin) + try it on linux and windows
- screenshot
- write text
- submit game ld, jmonkey, indiemag


# Arghh
- can"t suspend/resume particle emission from regular jme particle emitter (without freezing existing one or remove them)
