#import "../lib.typ": *

= Database SQL 

== Struttura database SQL

I requisiti del progetto variano leggermente tra le versioni`pure_html` e `ria`: in quest’ultima, infatti, è richiesto che i brani possano avere un ordine personalizzato all’interno della playlist a cui appartengono — funzionalità ottenuta tramite una semplice modifica nello schema delle tabelle SQL.

L’applicazione si appoggia dunque a un database relazionale MySQL (schema tiw2025) contenente quattro tabelle principali: *user, track, playlist, playlist_tracks*.

#v(1.5cm)


#figure(
  image("../img/uml/uml_bluetto.png", width: 11cm),
  caption: [UML diagram],
)<uml-diagram>

#v(5cm)

Qui di seguito mostriamo i diagrammi ER per entrambi i progetti; possiamo notare ancora come l'unica variazione tra i due progetti è la presenza di `custom_order` per la versione `RIA`

#v(1.3cm)

#figure(
  image("../img/er/er_diagram.svg", width: 100%),
  caption: [diagramma ER (HTML)],
)<er-diagram>


#v(1.3cm)


#figure(
  image("../img/er/er_diagram_ria.svg", width: 95%),
  caption: [diagramma ER (RIA)],
)<er-diagram-ria>

#v(5cm)

== Tabelle SQL

Mostriamo ora come sono state strutturate le tabelle:

- *user*
```sql
CREATE TABLE user
(
    user_id  integer     not null auto_increment,
    nickname varchar(32) not null unique,
    password varchar(64) not null,
    name     varchar(32) not null,
    surname  varchar(32) not null,

    primary key (user_id)
);
```
l'attributo `user_id` rappresenta la chiave primaria; l'unico altro attributo con un vincolo di unicità è nickname. 

- *track*
```sql
CREATE TABLE track
(
    track_id       integer       not null auto_increment,
    user_id        integer       not null,
    title          varchar(128)  not null,
    album_title    varchar(128)  not null,
    artist         varchar(64)   not null,
    year           year          not null,
    genre          varchar(64),
    song_checksum  char(64)      not null default '0...0',
    image_checksum char(64)      not null default '0...0',
    song_path      varchar(1024) not null,
    image_path     varchar(1024) not null,

    primary key (track_id),
    foreign key (user_id) REFERENCES user (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    unique (user_id, song_checksum),
    unique (user_id, title, artist),
    check (genre in ('Classical', 'Rock', 'Edm', 'Pop', 'Hip-hop', 'R&B', 'Country', 'Jazz', 'Blues', 'Metal', 'Folk', 'Soul', 'Funk', 'Electronic', 'Indie', 'Reggae', 'Disco'))
);
```
è stato introdotta una funzionalità speciale: il checksum SHA256 per il brano e l’immagine dell’album, che consente al server di salvare una sola copia di ciascun file, evitando duplicati basati solo sul nome.

La tabella include un vincolo di unicità su `user_id`, `song_checksum` per garantire questa proprietà, e su `user_id`, `title`, `artist` per evitare duplicati interni. Ogni traccia è infine collegata in modo univoco al suo utente tramite chiave esterna. #footnote[Un utente non può avere tracce duplicate]

#v(1cm)

- *playlist*
```sql
CREATE TABLE playlist
(
    playlist_id    integer     not null auto_increment,
    playlist_title varchar(32) not null,
    creation_date  date        not null default CURRENT_DATE,
    user_id        integer     not null,

    primary key (playlist_id),
    unique (playlist_title, user_id),
    foreign key (user_id) REFERENCES user (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);
```
L’attributo `creation_date` assume come valore predefinito la data odierna; inoltre, il vincolo di unicità su `playlist_title`, `user_id` assicura che ogni utente possa avere una sola playlist con lo stesso titolo, collegata a lui tramite chiave esterna.


- *playlist_tracks* - per pure_HTML
```sql
CREATE TABLE playlist_tracks - per 
(
    playlist_id  integer not null,
    track_id     integer not null,

    primary key (playlist_id, track_id),
    foreign key (playlist_id) REFERENCES playlist (playlist_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    foreign key (track_id) REFERENCES track (track_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);
```<playlist-tracks-code>

Questa tabella rappresenta la relazione “Contained in” nel diagramma ER (@er-diagram). La sua chiave primaria è composta (l’unica del progetto) e serve a collegare un brano a una playlist. A differenza delle altre tabelle, che richiedevano una chiave primaria e un vincolo di unicità distinti, qui una chiave composta è la scelta corretta, poiché uno stesso brano può appartenere a più playlist.

- #ria() *playlist_tracks* - per RIA
```sql
CREATE TABLE playlist_tracks
(
    playlist_track_id integer auto_increment,
    playlist_id       integer not null,
    track_id          integer not null,
    custom_order      integer,

    primary key (playlist_track_id),
    unique (playlist_id, track_id),
    foreign key (playlist_id) references playlist (playlist_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    foreign key (track_id) references track (track_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);
```<playlist-tracks-code>

Analogamente al codice precedente, anche questa tabella rappresenta la relazione “Contained in” nel diagramma ER della versione RIA (@er-diagram-ria), con l’aggiunta dell’attributo custom_order. La precedente chiave primaria è stata convertita in vincolo di unicità, come in altre parti del progetto, mentre il resto della struttura rimane invariato.


