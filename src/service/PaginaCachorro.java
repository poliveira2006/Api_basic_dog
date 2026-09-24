package service;

import api.DogApi;
import java.util.HashMap;
import java.util.Map;

public class PaginaCachorro {

    public static void main(String[] args) {
        // Simula $_GET['nomeCachorro']
        Map<String, String> get = parseQueryString(args);
        // Exemplo de teste:
        // get.put("nomeCachorro", "siberian husky");

        String imagem = "";
        DogApi service = new DogApi();

        String nomeCachorro = get.get("nomeCachorro");

        if (nomeCachorro != null && !nomeCachorro.isEmpty()) {

            String nomeBreed = nomeCachorro.replace(' ', '-');

            String[] breedArr = nomeCachorro.split(" ");
            String breed = breedArr.length > 1 ? breedArr[1] : breedArr[0];

            imagem =
                    "<div>" + ucfirst(nomeBreed) + "</div>" +
                    "<img src=\"" + service.getImage(breed) + "\" " +
                    "alt=\"dog\" class=\"imagem-cachorro\"></img>";
        }

        System.out.println(imagem);
    }

    /** Equivalente ao ucfirst() do PHP. */
    private static String ucfirst(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    /** Converte args do tipo chave=valor em Map, simulando $_GET. */
    private static Map<String, String> parseQueryString(String[] args) {
        Map<String, String> params = new HashMap<>();
        if (args == null) return params;
        for (String arg : args) {
            int idx = arg.indexOf('=');
            if (idx > 0) {
                params.put(arg.substring(0, idx), arg.substring(idx + 1));
            }
        }
        return params;
    }
}