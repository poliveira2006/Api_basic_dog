package service;

import api.DogApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListaRacas {

    public static void main(String[] args) {

        List<String> breedList = new ArrayList<>();
        DogApi service = new DogApi();
        Map<String, List<String>> response = service.allBreeds();

        for (Map.Entry<String, List<String>> entry : response.entrySet()) {
            String breed = entry.getKey();
            List<String> prefixes = entry.getValue();

            if (prefixes != null && !prefixes.isEmpty()) {
                for (String prefix : prefixes) {
                    breedList.add(prefix + " " + breed);
                }
            } else {
                breedList.add(breed);
            }
        }

        // Exibe o resultado (equivalente a imprimir/retornar a lista)
        for (String b : breedList) {
            System.out.println(b);
        }
    }
}