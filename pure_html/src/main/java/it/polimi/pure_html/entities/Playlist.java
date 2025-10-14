package it.polimi.pure_html.entities;

/**
 * Rappresenta una playlist caricata nel sistema.
 * Contiene le informazioni principali necessarie ai controller e ai DAO
 * per gestire le operazioni lato server.
 */
public record Playlist(
        int id,
        String title,
        java.sql.Date creation_date,
        User user
) {
}
