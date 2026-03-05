package com.alurachallenge.literalura.model;

public enum Idioma {
    INGLES("en", "Inglés"),
    ESPANOL("es", "Español"),
    PORTUGUES("pt", "Portugués"),
    RUSO("ru", "Ruso");


    private String idiomaRandom;
    private String idiomaEspanol;

    Idioma(String idiomaRandom , String idiomaEspanol) {
        this.idiomaRandom  = idiomaRandom;
        this.idiomaEspanol = idiomaEspanol;
    }

    public static Idioma fromString(String text){
        for (Idioma idioma : Idioma.values()){
            if (idioma.idiomaRandom.equalsIgnoreCase(text)){
                return idioma;
            }
        }
        throw  new IllegalArgumentException("Ninguna idioma encontrado " +text);
    }

    public static Idioma fromEspanol(String text){
        for (Idioma idioma : Idioma.values()){
            if (idioma.idiomaEspanol.equalsIgnoreCase(text)){
                return idioma;
            }
        }
        throw  new IllegalArgumentException("Ninguna idioma encontrado " +text);
    }
}
