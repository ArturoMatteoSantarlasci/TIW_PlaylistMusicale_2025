package it.polimi.pure_html.entities;

public record Track(
        int id,
        int user_id,
        String title,
        String artist,
        Integer year,
        String album_title,
        String genre,
        String image_path,
        String song_path,
        String song_checksum,
        String image_checksum
) {
}
