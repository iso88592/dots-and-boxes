# Dots and Boxes

A tournament server for the classic [Dots and Boxes](https://en.wikipedia.org/wiki/Dots_and_boxes) game. Players write bots in C#, push them to their own Docker container via Git, and the server runs matches automatically. There's a live dashboard with animated GIFs of each game and a leaderboard.

I built this for a workshop where participants compete against each other's bots in real time.

![Java](https://img.shields.io/badge/Java-24-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![.NET](https://img.shields.io/badge/.NET-9.0-purple)

## How it works

1. A participant opens the dashboard and clicks "Join"
2. They paste their SSH public key — the server spins up a Docker container with a Git repo inside
3. They clone the repo, write a C# bot implementing `IDotsAndBoxes`, and push
4. A Git hook inside the container builds and runs the bot automatically
5. The bot registers itself with the tournament server
6. The server pairs up bots and runs matches: 3 rounds on board sizes 3, 5, and 7 (9 games per matchup)
7. Results and game GIFs show up on the dashboard

Bots communicate with the server over HTTP. Each bot gets a `/turn` endpoint that receives the board state as JSON and must return a valid move within 10 seconds. Three strikes and you lose the game.

## Running it

Prerequisites: Java 24, Docker, Common network host

```bash
# Build the client Docker image first
bash create-docker.sh

# Run the server
./mvnw spring-boot:run
```

The dashboard is at `http://localhost:8080`. If you're running this on a LAN for a workshop, you'll also need to set up network routing — see `routing.sh`.

## Running tests

```bash
./mvnw test

# Single test class
./mvnw test -Dtest=ClientScoringTests
```

## Scoring

- Win: +1
- Loss: 0
- Invalid move (bad state, timeout, crash): -1

A "win" means you filled more cells than your opponent. The server validates every move — you can only draw one edge per turn, and you can't overwrite existing edges.

## The board

The game state is a `(2n+1) x (2n+1)` grid where `n` is the board size. Edges live at odd coordinates, cells at even coordinates. `-1` means undrawn, `0` is red (player 1), `1` is green (player 2). When all four edges around a cell are drawn, the cell gets filled automatically for whoever drew the last edge.

## Project structure

The server is a Spring Boot app. The interesting bits:

- `TournamentService` - runs the match loop, pairs clients, manages the game queue
- `OrchestratorService` - Docker container lifecycle (create, configure SSH/Git, destroy)
- `ClientService` - tracks registered bots and their scores
- `GameState` - board representation, move validation, JSON serialization for Java/C# interop
- `GameGif` - records each game as an animated GIF

The C# client template lives in `DotsAndBoxesClient/` and `docker/InitialRepo/`. Bots implement `IDotsAndBoxes` from the provided library. If you have multiple implementations, only the first one alphabetically will compete.
You can infer the necessary API calls from the C# application, so you can submit your own implementation in other languages. Just don't forget to update the git hook.

