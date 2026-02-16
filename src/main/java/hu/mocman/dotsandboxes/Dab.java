package hu.mocman.dotsandboxes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@Tag(name = "Tournament API", description = "Client registration and game replay endpoints")
public class Dab {

    private final ClientService clientService;
    private final TournamentService tournamentService;

    public Dab(ClientService clientService, TournamentService tournamentService) {
        this.clientService = clientService;
        this.tournamentService = tournamentService;
    }

    @Operation(
            summary = "Register a client bot",
            description = "Called by a bot container on startup to register itself with the tournament server. "
                    + "The server resolves the caller's IP from the request and begins polling the bot's /info endpoint."
    )
    @ApiResponse(responseCode = "200", description = "Registration accepted")
    @GetMapping("/dab/hello")
    public String register(HttpServletRequest request,
                           @Parameter(description = "Unique container ID assigned during provisioning") @RequestParam String id) {
        clientService.register(request, id);
        log.info("Client updated. Starting tournament.");
        return "{\"result\": \"ok\"}";
    }

    @Operation(
            summary = "Get a game replay GIF",
            description = "Returns an animated GIF recording of a completed game. "
                    + "The ID format encodes the two player initials, board size, and round number."
    )
    @ApiResponse(responseCode = "200", description = "GIF image data",
            content = @Content(mediaType = MediaType.IMAGE_GIF_VALUE))
    @GetMapping(value = "/gifs/{id}", produces = MediaType.IMAGE_GIF_VALUE)
    public byte[] getGif(@Parameter(description = "Game GIF identifier") @PathVariable String id) {
        return tournamentService.getGif(id);
    }
}
