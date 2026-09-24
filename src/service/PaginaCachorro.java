package service;

import api.DogApi;

public class PaginaCachorro {

    // Referência à API para poder pedir a imagem de uma raça.
    private final DogApi api;

    public PaginaCachorro(DogApi api) {
        this.api = api;
    }

    public String gerarHtml(String nomeCachorro) {
        // Se o nome veio vazio, devolve uma string vazia.
        if (nomeCachorro == null || nomeCachorro.isEmpty()) {
            return "";
        }

        // Troca os espaços por hífen 
        String nomeBreed = nomeCachorro.replace(' ', '-');

        // Divide o nome em palavras 
        String[] breedArr = nomeCachorro.split(" ");

        // Se tem mais de uma palavra, a raça principal é a última; senão, é a única.
        String breed = breedArr.length > 1 ? breedArr[1] : breedArr[0];

        // Monta o HTML com o nome formatado e a URL da imagem.
        return "<div>" + ucfirst(nomeBreed) + "</div>" +
               "<img src=\"" + api.getImage(breed) + "\" " +
               "alt=\"dog\" class=\"imagem-cachorro\"></img>";
    }

    // Deixa a primeira letra  em maiúscula.
    private String ucfirst(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}