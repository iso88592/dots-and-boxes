package hu.mocman.dotsandboxes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
public class ClientServiceImpl implements ClientService {


    private final TournamentService tournamentService;

    public ClientServiceImpl(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    private HashMap<String, Client> clients = new HashMap<>();
    @Override
    public void register(HttpServletRequest request, String id) {
        Client client = new Client(request);
        client.update(tournamentService, this);
        clients.put(id, client);
    }

    @Override
    public List<Client> getClients() {
        List<Client> result = new ArrayList<>(clients.values());
        result.sort(Comparator.comparingInt(Client::getScore).reversed());
        return result;
    }

}
