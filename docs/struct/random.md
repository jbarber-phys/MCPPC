# Random
Random is used to generate random numbers with the `/random` command.

An instance of Random represents a `sequenceId` (instead of an nbt value). The struct can also be used statically to get random numbers that do not have a `sequenceId`

## Static and Nonstatic methods
### `T uniform<type T>(T min (optional), T max (optional))`
Returns a uniformly distributed random value from `min` (inclusuve) to `max` (exclusive). If only 1 argument is given, that is `max` (and min is set to zero). If no arguments are given, then the default range depends on the type (0.0 to 1.0 for floats), but goes from 0 to max integer for int types.

## setSeed(num seed)
Resets the sequence with the given seed.

It appears to also be given time information so resets to the same seed and sequence will give different resulting numbers.