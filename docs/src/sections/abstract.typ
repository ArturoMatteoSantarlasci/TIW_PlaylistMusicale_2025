#import "../lib.typ":*

#set align(horizon)

= Abstract

/ Panoramica: TIW Playlist Musicale 2025 è un’applicazione web per la gestione di brani musicali e playlist personali, sviluppata come progetto didattico al *Politecnico di Milano* (Tecnologie Internet e Web). Il codice sorgente è reperibile su #link("https://github.com/ArturoMatteoSantarlasci/TIW_PlaylistMusicale_2025")[GitHub]. L’app permette agli utenti di registrarsi e autenticarsi, caricare i propri brani musicali (inclusi file audio e copertine) e organizzare questi brani in playlist personalizzate. Le funzionalità includono la creazione di nuove playlist, l’aggiunta/rimozione di brani dalle playlist, la riproduzione audio con un mini-player integrato e la ricerca/filtraggio di brani.

Il progetto è in realtà formato da due sottoprogetti: una versione *pure_HTML*, strutturata come una serie di pagine web separate, e una versione *RIA*, strutturata come una webapp a pagina singola con aggiornamenti dinamici lato client. Le funzionalità sono pressocchè le stesse; le differenze di codice riguardano principalemnte il livello di frontend.

/ Strumenti: Sono state adottate le seguenti tecnologie: #text(fill: rgb("#5283A2"), weight: "bold", "Java") per il backend del server, sfruttando le API di Jakarta; #text(fill: rgb("#ae8e26"), weight: "bold", "Apache Tomcat") per l’esecuzione del server; per la versione HTML, #text(fill: rgb("#005F0F"), weight: "bold", "Thymeleaf"), un motore di template; mentre per la versione RIA, #text(fill: rgb("#dcca3f"), weight: "bold")[Javascript]. Per il database, #text(fill: rgb("#192C5F"), weight: "bold")[MariaDB] è stato scelto come DBMS, e la comunicazione con il server avviene tramite #text(fill: rgb("#007396"), weight: "bold", "JDBC"). 

La documentazione è stata realizzata mediante l'utilizzo di #text(fill: eastern, weight: "bold")[Typst], il successore di LaTeX. Inoltre, per la creazione dei diagrammi sequenziali è stato utilizzato il pacchetto `chronos`. 

È un progetto Maven, il quale scaricherà automaticamente tutte le dipendenze corrette (come Thymeleaf e il driver JDBC). È stato scelto di utilizzare #text(fill: rgb("#d04f2a"), weight: "bold")[IntelliJ IDEA Ultimate Edition]. Dopo aver verificato che tutte le dipendenze siano state installate correttamente, esegui la configurazione di Tomcat desiderata e lascia che il server venga distribuito. Sarà accessibile all’indirizzo:

#align(
  center,
  link("http://localhost:8080/pure_html_war_exploded", "http://localhost:8080/[version]_war_exploded") + [: `[version]` può essere `pure_html` oppure `js`],
)

I file degli *upload* vengono cariati in una cartella interna al computer, in locale, così da non salvarli nel codice sorgente.

Di seguito vengono presentate le specifiche, l’architettura del database, il riepilogo del codice per entrambe le versioni, alcuni diagrammi sequenziali delle principali interazioni, i filtri applicativi, la gestione dello stile (CSS) e una panoramica delle tecnologie utilizzate.

