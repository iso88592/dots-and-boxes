package hu.mocman.dotsandboxes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Controller
public class Dashboard {
    private final ClientService clientService;
    private final OrchestratorService orchestratorService;
    private final TournamentService tournamentService;

    public Dashboard(ClientService clientService, OrchestratorService orchestratorService, TournamentService tournamentService) {
        this.orchestratorService = orchestratorService;
        this.clientService = clientService;
        this.tournamentService = tournamentService;
        tournamentService.startTournament();
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("clients", clientService.getClients());
        return "dashboard";
    }

    @GetMapping("/join")
    public String join(Model model) {
        model.addAttribute("container", null);
        return "join";
    }

    @GetMapping("/join/{id}")
    public String showInfo(@PathVariable String id, Model model) {
        String ip = orchestratorService.getContainerIp(id);
        model.addAttribute("container", ip);

        return "join";
    }

    @PostMapping("/join")
    public String join(@RequestBody String sshKey) {
        String key = URLDecoder.decode(sshKey.split("=")[1], StandardCharsets.UTF_8);
        String id = orchestratorService.spawn(key);
        return "redirect:/join/" + id;
    }
}
