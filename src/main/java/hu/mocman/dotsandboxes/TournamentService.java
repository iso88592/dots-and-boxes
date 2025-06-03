package hu.mocman.dotsandboxes;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;

import java.util.List;

@Service
public interface TournamentService {
    void startTournament(List<Client> clients, Client client);

    GameState getLatestGameState();

    byte[] getGif(String id);

    List<String> getGifs();

    void reset();

    List<Tuple<Client,Client>> getPairs();

    void disableProcessing();

    void enableProcessing();

    void processSingleMatch();

    interface TournamentEventListener {
        void onPairPopped(Tuple<Client, Client> pair);

        void onPairProcessed(Tuple<Client, Client> pair);

    }

}
