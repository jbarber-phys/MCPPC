# particles
A singleton containing various builtin functions for generating lots of particles in a certain shape.
## Particle expressions
The first argument in any of the member functions will be a particle expression. It appears exactly as it does in the `/particle` command, with all parameters like color appearing afterward seperated by whitespace.
```mcpp
particles.line(minecraft:dust 1.0 0.0 0.0 1.0, ~ ~ ~, ~ ~3 ~, 10)
```
All member founctions contain a second argument which is the `force|normal` arg. Just put either `force` or `normal` as the arg.

## Builtin Functions

### Line (particle, force|normal, selector showTo, pos start, pos end, num count, num speed (optional))
Creates particles in a line.
### Ring (particle, force|normal, selector showTo, num radius, pos velocity, num count)
Creates particles in a ring (in the xz plane).

The velocity will be given to all particles. If the velocity is in caret notation, then the dimensions will be local cylindrical coordinates: radial, vertical, and angular (in that order). 
```mcpp
particles.ring(minecraft:flame,force,@a,3,^ ^0.5 ^,20);
//ring flying upward
particles.ring(minecraft:flame,force,@a,3,^-1 ^ ^,20);
//ring imploding
```
### Sphere (particle, force|normal, selector showTo, num radius, pos velocity, num count)
Creates particles in a sphere. The `count` is only an appriximation, a different number of particles may appear.

The velocity will be given to all particles. If the velocity is in caret notation, then the dimensions will be local spherical coordinates: radial, theta angular, and phi angular (in that order). There is 1 key difference from normal spherical coordinates: theta is zero at the equator and increases as y increases.
```mcpp
particles.sphere(minecraft:flame,force,@a,0,^1 ^ ^,100);
//expanding fireball
```