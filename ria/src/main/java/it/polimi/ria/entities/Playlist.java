package it.polimi.ria.entities;

public record Playlist(
        int id,
        String title,
        java.sql.Date creation_date,
        User user
) {
}