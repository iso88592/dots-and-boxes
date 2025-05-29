package hu.mocman.dotsandboxes;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.util.Tuple;

@Service
public interface TournamentService {
    void startTournament();
    public interface TournamentEventListener {
        void onPairPopped(Tuple<Client, Client> pair);

        void onPairProcessed(Tuple<Client, Client> pair);
    }

}
