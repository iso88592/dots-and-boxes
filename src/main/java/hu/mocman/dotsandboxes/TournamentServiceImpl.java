package hu.mocman.dotsandboxes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;

import java.util.*;

@Slf4j
@Service
public class TournamentServiceImpl implements TournamentService {

    private final ClientService clientService;
    private final Queue<Tuple<Client, Client>> roundPairs = new LinkedList<>();
    private boolean tournamentActive = false;
    private Thread processingThread;
    private final List<TournamentEventListener> eventListeners = new ArrayList<>();

    public TournamentServiceImpl(ClientService clientService) {
        this.clientService = clientService;
        initProcessingThread();
    }

    @Override
    public void startTournament() {
        tournamentActive = true;
        createPairs();
        processingThread.start();
    }

    private void createPairs() {
        List<Client> clients = clientService.getClients();
        for (int i = 0; i < clients.size(); i++) {
            for (int j = 0; j < clients.size(); j++) {
                if (i == j) continue;
                if (roundPairs.contains(new Tuple<>(clients.get(i), clients.get(j)))) continue;
                if (roundPairs.contains(new Tuple<>(clients.get(j), clients.get(i)))) continue;
                roundPairs.add(new Tuple<>(clients.get(i), clients.get(j)));
            }
        }
    }


    private void initProcessingThread() {
        processingThread = new Thread(() -> {
            while (tournamentActive) {
                try {
                    synchronized (roundPairs) {
                        while (roundPairs.isEmpty()) {
                            roundPairs.wait();
                        }
                        Tuple<Client, Client> pair = roundPairs.poll();
                        for (TournamentEventListener listener : eventListeners) {
                            listener.onPairPopped(pair);
                        }
                        process(pair);
                        for (TournamentEventListener listener : eventListeners) {
                            listener.onPairProcessed(pair);
                        }
                        if (roundPairs.isEmpty()) {
                            roundPairs.notifyAll();
                        } else {
                            roundPairs.wait();
                        }
                        if (roundPairs.isEmpty()) {

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

    private boolean coinFlip() {
        return random.nextBoolean();
    }

    private void process(Tuple<Client, Client> contestants) {
        boolean isRedTurn = true;

        Client[] clients = new Client[2];

        for (int boardSize = 5; boardSize <= 11; boardSize += 2) {
            for (int rounds = 0; rounds < 3; rounds++) {
                if (coinFlip()) {
                    clients[0] = contestants._1();
                    clients[1] = contestants._2();
                } else {
                    clients[0] = contestants._2();
                    clients[1] = contestants._1();
                }

                GameState gameState = GameState.NewGame(boardSize);

                while (!gameState.gameOver()) {
                    Client currentPlayer = isRedTurn ? clients[0] : clients[1];
                    GameState newState = currentPlayer.turn(gameState.clone());
                    if (newState.isValidMove(gameState)) {
                        gameState = newState;
                        isRedTurn = !isRedTurn;
                    }
                }
            }
        }
    }

    

}
