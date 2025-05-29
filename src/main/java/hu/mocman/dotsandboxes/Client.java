package hu.mocman.dotsandboxes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Data
public class Client {
    private String id;
    private String address;
    private String publicAddress;
    private int score;
    private boolean active;

    public Client(HttpServletRequest request) {
        active = true;
        id = request.getParameter("id");
        address = request.getRemoteAddr();
    }

    private String names;

    public void update() {
        new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                log.info("Updating client with id {}", id);
                try {
                    Thread.sleep(100);
                    if (getUpdate()) {
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
            return true;
        } catch (IOException | JSONException e) {
            log.info(e.getMessage());
        }
        return false;
    }

    public GameState turn(GameState fromState) {
        return fromState;
    }
}
