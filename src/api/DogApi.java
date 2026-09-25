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

    //buscar  imagem aleatória de uma raça.
    private static final String BASE_URL_IMAGE = "https://dog.ceo/api/breed/%s/images/random";

    // devolve a lista completa de todas as raças e sub-raças disponíveis.
    private static final String BASE_URL_BREEDS = "https://dog.ceo/api/breeds/list/all";

    // Cliente HTTP criado uma única vez para reaproveitar conexões com o servidor.
    private final HttpClient httpClient;

    public DogApi() {
        //cria o cliente HTTP
        this.httpClient = HttpClient.newHttpClient();
    }

    public String getImage(String breed) {
        try {
            //monta a URL final substituindo o %s pelo nome da raça em minúsculo.
            String url = String.format(BASE_URL_IMAGE, breed.toLowerCase());

            // Prepara a requisição HTTP do tipo GET apontando para essa URL.
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            //executa a requisição e guarda a resposta em String
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            //Procura o campo "message" dentro do JSON devolvido pela API.
            Pattern pattern = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]+)\"");
            //pattern = padrão de busca
            Matcher matcher = pattern.matcher(response.body());
            //matcher aplica esse padrão

            // Se encontrou, devolve o conteúdo capturado entre as aspas.
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (IOException | InterruptedException e) {
            //exception
            Thread.currentThread().interrupt();
            e.printStackTrace();
            //objeto e(interrupção) exibe o erro no console
        }
        return "";
    }

    public Map<String, List<String>> allBreeds() {
        //map que vai guardar todas as raças e sua lista de sub-raças.
        Map<String, List<String>> result = new HashMap<>();

        try {
          
          //instancia a request com a requisição GET, utilizando o builder para criar a URL base das raças.
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL_BREEDS))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            //captura todo o bloco que está entre as chaves do campo "message".
            Pattern messagePattern = Pattern.compile(
                    "\"message\"\\s*:\\s*\\{(.*?)\\}\\s*\\}",
                    Pattern.DOTALL
            );
            Matcher messageMatcher = messagePattern.matcher(response.body());

            // Se não encontrou o bloco, devolve o mapa vazio.
            if (!messageMatcher.find()) {
                return result;
            }

            // Pega só o conteúdo interno das chaves, sem o "message"
            String bloco = messageMatcher.group(1);
            //formato nome e aplica esse formato no bloco
            Pattern entryPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\\[([^\\]]*)\\]");
            Matcher entryMatcher = entryPattern.matcher(bloco);

            while (entryMatcher.find()) {
                //enquanto achar o formato certo, insere as raças e subraças em grupos.
                String breed = entryMatcher.group(1);
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

                //guarda no mapa a raça e sua lista de sub-raças.
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