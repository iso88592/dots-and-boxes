package hu.mocman.dotsandboxes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
public class Dab {

    private final ClientService clientService;
    private final TournamentService tournamentService;

    public Dab(ClientService clientService, TournamentService tournamentService) {
        this.clientService = clientService;
        this.tournamentService = tournamentService;
    }
    @GetMapping("/dab/hello")
    public String register(HttpServletRequest request, @RequestParam String id) {
        clientService.register(request, id);
        log.info("Client updated. Starting tournament.");
        tournamentService.startTournament(clientService.getClients());

        return "{\"result\": \"ok\"}";
    }
}
