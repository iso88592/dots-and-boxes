package hu.mocman.dotsandboxes;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ClientService {

    void register(HttpServletRequest request, String id);

    List<Client> getClients();
}
