### Description

Customizable Behavior Tree driven AI that can perform tasks for you!
Eventually the goal is to have builders that will build required structures (homes, bakery, smithy, etc...) so that settlements will feel alive and dynamic.

The Roadmap is available in the wiki: https://github.com/Jugbot/settlements/wiki/Release-Plans

https://github.com/user-attachments/assets/3dff015d-d5cd-4203-bbaf-beb3fc99f874

### Setup

0. Install JDK 21
1. `./gradlew build`
2. `./gradlew fabric:runClient`

#### Gotchas

- When generating sources, you may have to manually reimport the build to metals if you are using vscode.

#### Useful links:

- https://fabricmc.net/wiki/documentation
- https://github.com/architectury/architectury-api/tree/1.21
- https://docs.architectury.dev

#### Debugging Tips

- In-game you can crouch+Rclick to get a list of actions performed by a settler in a single tick (excluding control structures like sequence, selector, etc.)
- Using Intellij, you can define a breakpoint to a problematic behavior.
- If you want to see the total list of actions after a certain behavior is reached you can 
  - mark a breakpoint at the action implementation
  - optionally mark it as not suspending
  - make a breakpoint where the `behaviorLog` is recorded that only suspends after the previous breakpoint is hit (under breakpoint Rclick -> more)
