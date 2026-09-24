package api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DogApi {

    private static final String BASE_URL = "https://dog.ceo/api/breed/%s/images/random";
    private final HttpClient httpClient;

    public DogApi() {
        this.httpClient = HttpClient.newHttpClient();
        //construtor
    }

    /**
     * Retorna a URL da imagem de uma raça de cachorro.
     *
     * @param breed nome da raça (ex: "husky")
     * @return URL da imagem ou string vazia em caso de erro
     */
    public String getImage(String breed) {
        try {
            String url = String.format(BASE_URL, breed.toLowerCase()); //recebe o nome do breed (raça) e devolve a imagem de acordo
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // Extrai o campo "message" do JSON de forma simples
            Pattern pattern = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(response.body());
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        return "";
    }

    public Map<String, List<String>> allBreeds() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'allBreeds'");
    }
}