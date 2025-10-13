#import "lib.typ": *
#import "properties.typ": *

#show: project.with(..properties)

#show: frontmatter.with()
#include "sections/abstract.typ"

#show: mainmatter.with()
#include "sections/consegna.typ"
#include "sections/specifiche.typ"
#include "sections/databaseSQL.typ"
#include "sections/struttura_codice.typ"
#include "sections/diagrammi_di_sequenza.typ"
