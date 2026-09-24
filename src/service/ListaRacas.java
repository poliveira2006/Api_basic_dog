package service;

import api.DogApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListaRacas {

    public static void main(String[] args) {

        List<String> breedList = new ArrayList<>();
        //cria a arrayList breedList
        DogApi service = new DogApi();
        //metodo service 
        Map<String, List<String>> response = service.allBreeds();
        //instancia response como service.allBreads()
        for (Map.Entry<String, List<String>> entry : response.entrySet()) {
            String breed = entry.getKey();
            List<String> prefixes = entry.getValue();
            //retorna o nome da raça e guarda em memória
            if (prefixes != null && !prefixes.isEmpty()) {
                for (String prefix : prefixes) {
                    breedList.add(prefix + " " + breed);
                }
            } else {
                breedList.add(breed);
            }
        }//verifica se a lista esta vazia, se nao adiciona o objeto a lista raça (breed)

        //Exibe o resultado da lista
        for (String b : breedList) {
            System.out.println(b);
        }
    }
}