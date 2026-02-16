package hu.mocman.dotsandboxes;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.InvalidArgumentException;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
public class TournamentServiceImpl implements TournamentService {

    private final BlockingQueue<Tuple<Client, Client>> roundPairs = new LinkedBlockingQueue<>();
    private Thread processingThread;
    private final List<TournamentEventListener> eventListeners = new ArrayList<>();
    private GameState latestGameState;

    public TournamentServiceImpl() {
        log.info("Starting TournamentServiceImpl");
        initProcessingThread();
        log.info("TournamentServiceImpl initialized");
        processingThread.start();
    }

    @Override
    public void startTournament(List<Client> clients, Client client) {
        createPairs(clients, client);
        if (!processingEnabled) {
            try {
                log.info("Consumer process should consume and re-add the first pair.");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public GameState getLatestGameState() {
        return latestGameState;
    }

    @Override
    public byte[] getGif(String id) {
        for (GameGif gif : gifs) {
            if (gif.id.equals(id)) {
                return gif.gifData;
            }
        }
        return null;

    }

    @Override
    public List<String> getGifs() {
        List<String> result = new ArrayList<>();
        for (GameGif gif : gifs) {
            result.add(gif.id);
        }
        return result;
    }

    @Override
    public void reset() {
        roundPairs.clear();
        latestGameState = null;
        gifs.clear();
        currentGif = null;
    }

    @Override
    public List<Tuple<Client, Client>> getPairs() {
        return roundPairs.stream().toList();
    }

    @Override
    public void disableProcessing() {
        processingEnabled = false;
    }

    @Override
    public void enableProcessing() {
        processingEnabled = true;
    }

    @Override
    public void processSingleMatch() {
        try {
            Tuple<Client, Client> match = roundPairs.take();
            process(match);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void createPairs(List<Client> clients, Client client) {
        log.info("Create pairs");
        int i = 0;
        for (; i < clients.size(); i++) {
            if (clients.get(i).getAddress().equals(client.getAddress())) break;
        }
        for (int j = 0; j < clients.size(); j++) {
            if (i == j) continue;
            if (!clients.get(i).isReady()) continue;
            if (!clients.get(j).isReady()) continue;
            if (roundPairs.contains(new Tuple<>(clients.get(i), clients.get(j)))) continue;
            if (roundPairs.contains(new Tuple<>(clients.get(j), clients.get(i)))) continue;
            log.info("Adding pair {} and {}", clients.get(i), clients.get(j));
            roundPairs.add(new Tuple<>(clients.get(i), clients.get(j)));
        }
    }

    private boolean processingEnabled = true;

    private void initProcessingThread() {
        processingThread = new Thread(() -> {
            while (true) {
                try {
                    synchronized (roundPairs) {
                        if (!processingEnabled) {
                            Thread.sleep(1000);
                            continue;
                        }
                        Tuple<Client, Client> pair = roundPairs.take();
                        if (!processingEnabled) {
                            log.info("Readding pair");
                            roundPairs.add(pair);
                            continue;
                        }
                        for (TournamentEventListener listener : eventListeners) {
                            listener.onPairPopped(pair);
                        }
                        try {
                            process(pair);
                        } catch (Exception e) {
                            log.info("Failed to process pair {}", pair, e);
                        }
                        for (TournamentEventListener listener : eventListeners) {
                            listener.onPairProcessed(pair);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private Random random = new Random();

    private boolean coinFlip(int round) {
        if (round == 0) return true;
        if (round == 1) return false;
        return random.nextBoolean();
    }

    private void process(Tuple<Client, Client> contestants) {
        log.info("Processing pair {}", contestants);
        boolean isRedTurn = true;

        Client[] clients = new Client[2];
        int[] scores = {0, 0};

        for (int boardSize = 3; boardSize <= 7; boardSize += 2) {
            for (int rounds = 0; rounds < 3; rounds++) {
                int p1idx = 0;
                int p2idx = 1;
                if (coinFlip(rounds)) {
                    clients[0] = contestants._1();
                    clients[1] = contestants._2();
                } else {
                    clients[0] = contestants._2();
                    clients[1] = contestants._1();
                    p1idx = 1;
                    p2idx = 0;
                }

                GameState gameState = GameState.NewGame(boardSize);
                int[] strokes = {0, 0};
                startGame(clients[0], clients[1], boardSize, rounds + 1);
                try {

                    while (!gameState.isGameOver()) {
                        Client currentPlayer = isRedTurn ? clients[0] : clients[1];
                        GameState newState = currentPlayer.turn(gameState.clone());
                        if (newState != null && gameState.isValidMove(newState)) {
                            gameState = newState;
                            if (!gameState.fill()) {
                                gameState.nextPlayer();
                            }
                            snapshot(gameState);
                            isRedTurn = !isRedTurn;
                        } else {
                            strokes[isRedTurn ? 0 : 1]++;
                            log.info("Invalid move for {}: {}, stoke {}", currentPlayer.getId(), gameState, strokes[isRedTurn ? 0 : 1]);
                            if (strokes[isRedTurn ? 0 : 1] >= 3) {
                                log.info("Player {} has failed to make a valid move in 3 rounds", currentPlayer.getId());
                                scores[isRedTurn ? p2idx : p1idx]--;
                                break;
                            }
                        }
                    }
                } catch (InvalidArgumentException | NullPointerException e) {
                    log.info("Invalid move for {}: {}", clients[isRedTurn ? 0 : 1].getId(), gameState);
                }
                if (gameState.isGameOver()) {
                    int p1 = gameState.countScores(0);
                    int p2 = gameState.countScores(1);
                    if (p1 > p2) {
                        scores[p1idx]++;
                    } else {
                        scores[p2idx]++;
                    }
                }
                finalizeGame();
            }
        }
        if (scores[0] == 9) scores[0]++;
        if (scores[1] == 9) scores[1]++;
        clients[0].setScore(scores[0], clients[1].getId());
        clients[1].setScore(scores[1], clients[0].getId());
        log.info("Pair processed. The score is {}:{} ({}:{})", scores[0], scores[1], clients[0].getId(), clients[1].getId());
    }

    private void finalizeGame() {
        currentGif.finalizeGif();
        gifs.add(currentGif);
        if (gifs.size() > 12) {
            gifs.remove(0);
        }
        currentGif = null;
    }

    private void snapshot(GameState gameState) {
        latestGameState = gameState;
        currentGif.addFrame(gameState);
    }

    private void startGame(Client client, Client client1, int boardSize, int rounds) {
        currentGif = new GameGif(client.getId(), client1.getId(), boardSize, rounds);
    }

    List<GameGif> gifs = new ArrayList<>();
    GameGif currentGif;
}
