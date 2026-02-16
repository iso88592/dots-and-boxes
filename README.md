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

## Networking

This is the part that will save you an hour of head-scratching. The setup assumes a LAN workshop where participants connect to a shared network and the server host runs Docker containers on their behalf. There are three networks in play, and traffic needs to flow between all of them.

### The three networks

1. **Host WiFi/LAN** (`hostif`) - the network your server machine is on. Participants connect here to reach the dashboard.
2. **Point-to-point link** (`p2pif`) - a direct link between your router and the server. This could be a wired ethernet interface. Participants' traffic arrives through this.
3. **Docker bridge** (`bridge`) - created automatically by the server on first startup as `dots_and_boxes_bridge`. All bot containers live here. The server talks to containers through the bridge gateway IP.

### Why it doesn't just work

Docker's default network isolation blocks external traffic from reaching containers. Participants need to SSH into their containers to push code, and the containers need to reach the Spring Boot server on port 8080. Out of the box, Docker's `DOCKER-USER` iptables chain drops forwarded packets from outside interfaces into the bridge network.

On top of that, containers need internet access to restore NuGet packages when building the .NET bot. Without masquerading on the host interface, outbound traffic from the bridge has no return path.

### What routing.sh does

Run it as root with three arguments:

```bash
sudo bash routing.sh <host interface> <p2p interface> <docker bridge>
```

For example:

```bash
sudo bash routing.sh wlp0s20f3 enp1s0 br-c372a625f75d
```

You can find the bridge interface name in the server logs on startup. The `OrchestratorService` logs the network interface name and gateway IP when it creates or finds the bridge.

The script does three things:

1. Sets the default FORWARD policy to ACCEPT, so the kernel doesn't drop packets moving between interfaces.
2. Adds an nftables masquerade rule on the host interface, so containers can reach the internet through the host's outbound connection.
3. Adds an iptables rule to `DOCKER-USER` allowing traffic from the P2P interface into the Docker bridge, so participants can SSH into their containers.

### A typical setup

![Network diagram](https://www.plantuml.com/plantuml/svg/VPDFRzim3CNl-XGFkQmv97DST2boA8frADPB42o7NNPX9DeMub06fJGRXdttmVyQ0mKANsZnnqSzIdgCYJITJxM9HoyDAPrm87iiDPp5FOKwA9AIn8QK9KqenmMTw_GS96tfn3W52R4Xosu2ATj4KjQClmO0VlIgAgnOPUQVCLTYjYDDtlg69CLFgAb9eOapK1pZAVpJFtjyRWKJQmTf5qwPFUihAqvaAo_SGR5RChOEPuVPYoHMuTIb_F1ZJA6jeTctO8-iEAXt9UDcjSwNlzjltk73SkzhqlHhIB114ldEPC-6jFgOoqSk_vZRZjos7Tum5onSkCZYOHW1DfZDp12FgTuuDcZofhTQaELVSiFBSWcMPXMpkHceiMZmk1c3TtK7NTZRTKvVyhjYVUUk0khBAYqUteQA3QeGqtk5IzraCS-Asxan4XAZgqGeXfu6U1hTHoYJhP0g7WyIUFKqRg-an1VwWvSTWc1pdzrdXiMXrHtKQouwyU6o0DDB1xAMOspr3U0eLeT0wa3YiDz_RpSV3Dgqr-kPjBo6SrdrAUpo7MelnslhHnRNFh7_)

The [PlantUML source](docs/network-diagram.puml) is in the repo if you need to modify it.

Participants access the dashboard on the server's LAN IP. When they SSH to push code, traffic goes from the P2P interface through the bridge to their container. When a container registers with the server, it hits the bridge gateway IP on port 8080, which is the server itself.

## API docs

Swagger UI is available at `/swagger-ui.html` when the server is running. The raw OpenAPI spec is at `/v3/api-docs`.

