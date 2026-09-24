package api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DogApi {

    // URL base para buscar uma imagem aleatória de uma raça. O %s é o espaço onde o nome da raça será inserido.
    private static final String BASE_URL_IMAGE = "https://dog.ceo/api/breed/%s/images/random";

    // URL que devolve a lista completa de todas as raças e sub-raças disponíveis.
    private static final String BASE_URL_BREEDS = "https://dog.ceo/api/breeds/list/all";

    // Cliente HTTP criado uma única vez para reaproveitar conexões com o servidor.
    private final HttpClient httpClient;

    public DogApi() {
        // Cria o cliente HTTP com configurações padrão do Java.
        this.httpClient = HttpClient.newHttpClient();
    }

    public String getImage(String breed) {
        try {
            // Monta a URL final substituindo o %s pelo nome da raça em minúsculo.
            String url = String.format(BASE_URL_IMAGE, breed.toLowerCase());

            // Prepara a requisição HTTP do tipo GET apontando para essa URL.
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            // Executa a requisição e guarda a resposta como texto.
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // Procura o campo "message" dentro do JSON devolvido pela API.
            Pattern pattern = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(response.body());

            // Se encontrou, devolve o conteúdo capturado entre as aspas.
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException | InterruptedException e) {
            // Se a thread foi interrompida, restaura o sinal para quem chamou.
            Thread.currentThread().interrupt();
            // Exibe o erro no console para depuração.
            e.printStackTrace();
        }
        // Se algo falhou, devolve string vazia em vez de quebrar o fluxo.
        return "";
    }

    public Map<String, List<String>> allBreeds() {
        // Mapa que vai guardar cada raça e sua lista de sub-raças.
        Map<String, List<String>> result = new HashMap<>();

        try {
            // Prepara a requisição GET para a URL que lista todas as raças.
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL_BREEDS))
                    .GET()
                    .build();

            // Envia a requisição e recebe o corpo da resposta como texto.
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // Captura todo o bloco que está entre as chaves do campo "message".
            Pattern messagePattern = Pattern.compile(
                    "\"message\"\\s*:\\s*\\{(.*?)\\}\\s*\\}",
                    Pattern.DOTALL
            );
            Matcher messageMatcher = messagePattern.matcher(response.body());

            // Se não encontrou o bloco, devolve o mapa vazio.
            if (!messageMatcher.find()) {
                return result;
            }

            // Pega só o conteúdo interno das chaves, sem o "message": { } externo.
            String bloco = messageMatcher.group(1);

            // Para cada entrada no formato "nome":[...] extrai o nome e o array.
            Pattern entryPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\[([^\\]]*)\\]");
            Matcher entryMatcher = entryPattern.matcher(bloco);

            while (entryMatcher.find()) {
                // Nome da raça principal.
                String breed = entryMatcher.group(1);
                // Conteúdo de dentro dos colchetes (as sub-raças).
                String inner = entryMatcher.group(2).trim();

                // Lista que vai guardar as sub-raças encontradas.
                List<String> subs = new ArrayList<>();

                // Se houver conteúdo, extrai cada nome entre aspas.
                if (!inner.isEmpty()) {
                    Pattern subPattern = Pattern.compile("\"([^\"]+)\"");
                    Matcher subMatcher = subPattern.matcher(inner);
                    while (subMatcher.find()) {
                        subs.add(subMatcher.group(1));
                    }
                }

                // Guarda no mapa a raça e sua lista de sub-raças.
                result.put(breed, subs);
            }

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }

        // Devolve o mapa montado (vazio se houve erro).
        return result;
    }
}