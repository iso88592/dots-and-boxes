package hu.mocman.dotsandboxes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Dashboard", description = "Web UI and container provisioning")
public class Dashboard {
    private final ClientService clientService;
    private final OrchestratorService orchestratorService;
    private final TournamentService tournamentService;

    public Dashboard(ClientService clientService, OrchestratorService orchestratorService, TournamentService tournamentService) {
        this.orchestratorService = orchestratorService;
        this.clientService = clientService;
        this.tournamentService = tournamentService;
    }

    @Operation(summary = "Tournament dashboard", description = "Main page showing the leaderboard and game replay GIFs.")
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("clients", clientService.getClients());
        model.addAttribute("gameState", tournamentService.getLatestGameState());
        model.addAttribute("gifs", tournamentService.getGifs().reversed());
        return "dashboard";
    }

    @Operation(summary = "Join page", description = "Shows the form for submitting an SSH public key to provision a new container.")
    @GetMapping("/join")
    public String join(Model model) {
        model.addAttribute("container", null);
        return "join";
    }

    @Operation(summary = "Container info", description = "Shows the provisioned container's IP address after a successful join.")
    @GetMapping("/join/{id}")
    public String showInfo(@Parameter(description = "Container ID returned after provisioning") @PathVariable String id, Model model) {
        String ip = orchestratorService.getContainerIp(id);
        model.addAttribute("container", ip);

        return "join";
    }

    @Operation(
            summary = "Provision a new container",
            description = "Accepts a form-encoded SSH public key, spawns a Docker container with a Git repo, "
                    + "and redirects to the container info page."
    )
    @ApiResponse(responseCode = "302", description = "Redirect to /join/{id} with the new container's ID")
    @PostMapping("/join")
    public String join(@RequestBody String sshKey) {
        String key = URLDecoder.decode(sshKey.split("=")[1], StandardCharsets.UTF_8);
        String id = orchestratorService.spawn(key);
        return "redirect:/join/" + id;
    }
}
