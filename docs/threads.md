# Threads

Threads are used to run mcfunction code over multiple ticks, with complicated flow control that can involve delays. It is especially usefull for custom bossfights.
## Defining
Threads can be define as below.
```mcpp
thread public countdown {
  printf("starting");
  public int count = 5;
} next public middle wait(30) while (count > 0) wait (10) {
  //enter after 30 ticks but loop every 10
  printf("%s",a);
  a = a-1;
} next {
  //enter after 30 ticks but loop every 10
  printf("finished.");
} next stop;

```
Threads are defined with statements seperating several blocks. The first statement starts with the keyword `thread` followed (o)ptionally) by an access (public/private) and a name. The [`synchronized`](#synchronized) keyword goes before the access. Following that are any supported execute statements for the whole thread. After all of these are the controls for the first code block.

Subsequent blocks start with the `next` keyword followed by the controls for that block.

The controls for a block start with an optional block access and name (used to goto that block or start at it). Ony public blocks can be used as entry points for starts / restarts outside this thread. Following the access (if present) are various statements that control block flow (see [Thread Flow Control](#thread-flow-control)).

In place of a block, the `then` keyword can be used to do nothing.

The function can end with the `stop`, `kill`, or `restart` keywords. This keyword determines what will happen when the execution reaches the end. Following this keyword, a call to the thread can optionally be made. The syntax is the same as calling a thread but with no thread name.
## Calling
Threads can be started like so:
```mcpp
start countdown();
```
Multiple instances of the thread can run at the same time (unless it is [`synchronized`](#synchronized)). To manipulate the execution seperately, we must recieve the entity that will execute the thread (with no execute statements, this will be a newly summoned marker that will automatically be killed on stop). This is the last argument in the start function (and acts as a reference).
```mcpp
Entity a;
Entity b;
start countdown(a);
start countdown(b);
//we will see 2 countdowns
```
Threads can be restarted / stopped as well, but unless the thread is [`synchronized`](#synchronized), a selector of executors is needed as an argument:
```mcpp
restart countdown(a);
stop countdown(b);
```
In a start / restart statement, the name of a thread block can be specified as an entry point for execution to start at. This goes after the executor but before the executor reference:
```mcpp
Entity c;
start countdown(middle, c);
restart coutdown(b,middle);
```

## Synchronized
Addingthe `synchronized` keyword after the `thread` keyword during declaration makes it so that there can only be 1 instance of the thread running at a time. Any previous calls will automatically stop when another start is done.

Synchronized thread that do not have execute `as` statements will not have an executor, so that argument can be omitted in calls.
## Execute As At
An `as` or `asat` statement can be specified, if either is present, the thread will execute as the selector on start (with a seperate instance per entity). Only one instance of a thread can be running per entity, though.

If a thread does not have an `as` statement but is also not `synchronized`, then the executor will be a new marker summoned on start (and killed after stop).

Inside the thread, the `this` keyword represents a selector that refers to the entity executing this instance of the thread. This will be the case even if it is used inside a nested `execute as` block. There will be no executor if the thread is both `synchronized` and has no `as` statement.
```mcpp
thread public rocket asat (@e[type=chicken,distance=..16]) {
    public int fuel = 100;
}
 next while(fuel > 0)
{
    fuel = fuel - 1;
    private Vec3d<3> vel -> this.Motion;
    vel = vel + Vec3d<3>(0,0.1,0);
}next kill;
start rocket();
```
## Thread Flow Control
Flow within threads is controlled in 2 ways: statements before each block, and statements within blocks that set `goto`, `wait`, `break`, and `exit`.

The following statements can appear before a thread block:
 - `public`|`private` name: give the block a name as an entry point or for `goto` statements.
 - `wait(delay)` : waits the specified number of ticks before running the next block.
 - `while(condition)` : runs this block as long as the condition is true. A delay between loops can be specified after this statement (and a delay appearing before this statment controls the delay into the loop from the previous block)
 - `until(condition)` : same as `while(!condition)`

Flow set by these statements can be overriden by `goto`, `wait`, `break`, and `exit` statements. These statements all act like assignments and do not interrupt execution.
- `break`: breaks from a loop. Compadible with depth statements.
 - `goto`: sets the next block to run, must be a name of a block. Should interrupt loops.
 - `wait`: sets delay in ticks until the next block will run.
 - `exit`: whether to stop the thread early at the end of this block.

 In addition, there are two types of [Special Blocks](#special-blocks) that handle events (they are not part of normal execution).
## Thread Variables
variable declarations can appear in threads. These variables can never be accessed outside the thread. Public variables can be accessed in any block, but private ones are limited to the block they were declared in.

If there could be more than 1 executor, then all variables will be stored as scores to make sure they are unique. If this cannot be done, an error will be thrown. There are two fixes for this. The first is to add the compile flag `-uuidLookup`. This will permit mcppc to create an nbt map that contains UUIDs as keys to all executor locals. If you do this, make sure to never use inline `/return` or `/kill` commands because they could leak entries in this map.

If only one copy of a variable is needed, then the `volatile` keyword can be added in the declaration.
```mcpp
thread hasLocals asat(@a){
    public int a; //fine
    public String b;//must use uuid lookup
    public volatile String c;//shared

    private int d = 0;//OK, value is always set
    public int vel -> this.Motion;//OK
}
```
In addition variables with masks, private variables set at declaration, and variables inside `synchronized` threads will not be forced to be unique.
## Special Blocks
<!-- Death, Tick; TODO-->
There are two types of blocks that are not part of normal flow: `tick` and `death`. They can be omitted or put anywhere (and execution will just flow around them). They handle various events. They must be the only block control present.
 - `tick`: runs every tick of execution before normal code
 - `death`: runs if the executor unexpectedly dies. Note that some locals may not exist when this runs.
 An example is below:
 ```mcpp
thread asat(@p) {
    //do something, maybe loop
} 
//more blocks
next death {
    //handle death
}next stop;
 ```

