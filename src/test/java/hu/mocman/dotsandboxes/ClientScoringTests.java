package hu.mocman.dotsandboxes;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class ClientScoringTests {
    @Autowired
    private TournamentService tournamentService;

    private List<Client> clients = new ArrayList<>();

    private Client addClient(String name) {
        Client client = Mockito.mock(Client.class);
        Mockito.when(client.getAddress()).thenReturn("127.0.0.1");
        Mockito.when(client.getNames()).thenReturn(name);
        Mockito.when(client.getId()).thenReturn(name);
        Mockito.when(client.isReady()).thenReturn(true);
        clients.add(client);
        return client;
    }

    @BeforeEach
    public void setup() {
        tournamentService.reset();
        tournamentService.disableProcessing();
        addClient("Alice");
        addClient("Bob");
    }

    @AfterEach
    public void tearDown() {
        tournamentService.reset();
        tournamentService.enableProcessing();
    }

    @Test
    public void clientScoresAreZeroInitially() {
        tournamentService.startTournament(clients, clients.get(0));
        Assertions.assertThat(clients.get(0).getScore()).isEqualTo(0);
        Assertions.assertThat(clients.get(1).getScore()).isEqualTo(0);
    }

    @Test
    public void thereIsExactlyOnePairWithTwoClients() {
        tournamentService.startTournament(clients, clients.get(0));
        Assertions.assertThat(tournamentService.getPairs()).hasSize(1);
    }

    @Test
    public void thereAreThreePairsWithThreeClients() {
        tournamentService.startTournament(clients, clients.get(0));
        addClient("Charlie");
        tournamentService.startTournament(clients, clients.get(2));
        Assertions.assertThat(tournamentService.getPairs()).hasSize(3);
    }

    @Test
    public void clientScoreIsNegativeWithNoValidMoves() {
        tournamentService.startTournament(clients, clients.get(0));
        tournamentService.processSingleMatch();

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        Mockito.verify(clients.get(0)).setScore(captor.capture(), Mockito.any());
        Assertions.assertThat(captor.getValue()).isLessThan(0);

        Mockito.verify(clients.get(1)).setScore(captor.capture(), Mockito.any());
        Assertions.assertThat(captor.getValue()).isLessThan(0);
    }
}
