package it.polimi.pure_html.entities;

/**
 * Modello che rappresenta l'utente autenticato all'interno dell'applicazione.
 * Le informazioni sono recuperate dal database e conservate in sessione.
 */
public record User(int id, String nickname, String password, String name, String surname) {
}
