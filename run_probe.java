import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class run_probe {
    public static void main(String[] args) {
        String endpoint = System.getenv("OPENHANDS_API_URL");
        String apiKey = System.getenv("OPENHANDS_API_KEY");
        
        if (endpoint == null || endpoint.isEmpty()) {
            System.out.println("OPENHANDS_API_URL is missing. Please inject it into the control plane.");
            return;
        }
        
        System.out.println("Resolving real OpenHands endpoint: " + endpoint);
        
        try {
            URL url = new URL(endpoint + "/api/version");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (apiKey != null && !apiKey.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                System.out.println("PROBE SUCCESS. Version Response: " + response.toString());
                System.out.println("Capability Status: PARTIALLY_VERIFIED");
            } else {
                System.out.println("PROBE FAILED. HTTP " + responseCode);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                System.out.println("Error Response: " + response.toString());
            }
        } catch (Exception e) {
            System.out.println("PROBE FAILED. Exception: " + e.getMessage());
        }
    }
}
