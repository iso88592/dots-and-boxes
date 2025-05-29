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

    public Dab(ClientService clientService) {
        this.clientService = clientService;
    }
    @GetMapping("/dab/hello")
    public String register(HttpServletRequest request, @RequestParam String id) {
//        log.info("Registering client from {}" , request.getRemoteAddr());
        clientService.register(request, id);
        return "{\"result\": \"ok\"}";
    }
}
