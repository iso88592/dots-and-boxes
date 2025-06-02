package hu.mocman.dotsandboxes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.InvalidArgumentException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

@Slf4j
@Data
public class Client {
    private String id;
    private String address;
    private String publicAddress;
    private boolean active;
    private HashMap<String, Integer> scores = new HashMap<>();

    public int getScore() {
        int count = 0;
        for (var entry : scores.entrySet()) {
            count += entry.getValue();
        }
        return count;
    }

    public Client(HttpServletRequest request) {
        active = true;
        id = request.getParameter("id");
        address = request.getRemoteAddr();
        ready = false;
    }

    public boolean ready;
    private String names;

    public void update(TournamentService tournamentService, ClientServiceImpl clientService) {
        new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                log.info("Updating client with id {}", id);
                try {
                    Thread.sleep(100);
                    if (getUpdate()) {
                        tournamentService.startTournament(clientService.getClients(), this);
                        return;
                    } else {
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    log.info(e.getMessage());
                }
            }
            active = false;
            log.info("Giving up.");
        }).start();

    }

    private boolean getUpdate() {
        try {
            URL url = new URL("http://" + address + ":5000/info");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            if (status != 200) return false;
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();
            connection.disconnect();

            JSONObject json = new JSONObject(response.toString());
            var extractedNames = json.getJSONArray("names");
            publicAddress = json.getString("ip");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < extractedNames.length(); i++) {
                builder.append(extractedNames.getString(i));
                if (i != 0) builder.append(", ");
            }
            names = builder.toString();
            ready = true;
            return true;
        } catch (IOException | JSONException e) {
            log.info(e.getMessage());
        }
        return false;
    }

    private GameState getNextTurn(GameState state) {
        try {
            URL url = new URL("http://" + address + ":5000/turn?state=" + URLEncoder.encode(state.convertStateToJson(), StandardCharsets.UTF_8));
            log.debug("Sending turn request to {}", url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(1000 * 10);
            int status = connection.getResponseCode();
            if (status != 200) {
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();
            connection.disconnect();

            log.debug("Received response: {}", response);

            return GameState.FromJson(response.toString());
        } catch (IOException | JSONException e) {

        }
        return null;
    }

    public GameState turn(GameState fromState) {
        final StateContainer stateContainer = new StateContainer();
        for (int retries = 3; retries > 0; retries--) {
            Thread turnThread = new Thread(() -> {
                stateContainer.state = getNextTurn(fromState);
            });
            try {
                turnThread.start();
                turnThread.join(1000 * 10);
            } catch (InterruptedException e) {
                log.info(e.getMessage());
            }
            if (turnThread.isAlive()) {
                turnThread.interrupt();
                log.info("Client did not respond in 1 minute");
            } else {
                return stateContainer.state;
            }
        }
        throw new InvalidArgumentException("Invalid state");
    }

    public void setScore(int score, String opponent) {
        scores.put(opponent, score);
    }

    private static class StateContainer {
        public GameState state = null;
    }
}
