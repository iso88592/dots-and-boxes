package hu.mocman.dotsandboxes;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@Service
public class ClientServiceImpl implements ClientService {
    private final OrchestratorService orchestratorService;

    public ClientServiceImpl(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    private HashMap<String, Client> clients = new HashMap<>();
    @Override
    public void register(HttpServletRequest request, String id) {
        Client client = new Client(request);
        client.update();
        clients.put(id, client);
    }

    @Override
    public List<Client> getClients() {
        List<Client> result = new ArrayList<>(clients.values());
        result.sort(Comparator.comparingInt(Client::getScore));
        return result;
    }
}
