#let colortheme = white

// Get current theme
#let theme-state = state("theme", (
  palette: (
    base00: "#ffffff",    //sfondo 
    base01: "#f6f8fa",
    base02: "#eaeef2",
    base03: "#c9d1d9",    //commenti
    base04: "#8b949e",
    base05: "#484f58",    //testo principale
    base06: "#30363d",
    base07: "#0d1117",
    base08: "#d73a49",    //variabili/errori
    base09: "#f66a0a",    //tipi / numeri
    base0A: "#d29922",
    base0B: "#28a745",
    base0C: "#79b8ff",
    base0D: "#0366d6",
    base0E: "#6f42c1",
    base0F: "#a04100",
  ),
))
// Individual colors from base-16

#let _col(v) = {
  if (type(v) == str) { rgb(v) } else { v }
}
#let get-default-background() = {
  _col(theme-state.at(here()).palette.base00)
}
#let get-lighter-background() = {
  _col(theme-state.at(here()).palette.base01)
}
#let get-selection-background() = {
  _col(theme-state.at(here()).palette.base02)
}
#let get-comments() = {
  _col(theme-state.at(here()).palette.base03)
}
#let get-dark-foreground() = {
  _col(theme-state.at(here()).palette.base04)
}
#let get-default-foreground() = {
  _col(theme-state.at(here()).palette.base05)
}
#let get-light-foreground() = {
  _col(theme-state.at(here()).palette.base06)
}
#let get-light-background() = {
  _col(theme-state.at(here()).palette.base07)
}
#let get-variables() = {
  _col(theme-state.at(here()).palette.base08)
}
#let get-data-types() = {
  _col(theme-state.at(here()).palette.base09)
}
#let get-support-types() = {
  _col(theme-state.at(here()).palette.base0A)
}
#let get-string() = {
  _col(theme-state.at(here()).palette.base0B)
}
#let get-support() = {
  _col(theme-state.at(here()).palette.base0C)
}
#let get-functions() = {
  _col(theme-state.at(here()).palette.base0D)
}
#let get-keywords() = {
  _col(theme-state.at(here()).palette.base0E)
}
#let get-deprecated() = {
  _col(theme-state.at(here()).palette.base0F)
}

// Document structure
#let project(
  title: "",
  subtitle: "",
  authors: (),
  columns: 1,
  tech-stack: false,
  body,
) = context {
  set document(
    title: title,
    author: authors.map(a => sym.copyright + " " + a.fullname),
    date: datetime(
      day: 15,
      month: 10,
      year: 2025,
    ),
  )

  // Given two colors, if the former is too bright, then the latter is returned
  let color_filter(correct, overridden) = {
    let correct_components = correct.rgb().components().slice(0, 3).map(x => x / 100% * 256)

    let red = correct_components.at(0)
    let green = correct_components.at(1)
    let blue = correct_components.at(2)

    if (red > 235 and green > 235 and blue > 235) {
      return overridden
    } else {
      return correct
    }
  }

  set page(
    margin: (
      rest: 1.65cm,
      top: 2.4cm,
    ),
    fill: color_filter(get-default-background(), colortheme.lighten(80%)),
    header: { },
    footer: { },
    numbering: "1",
  )

  set text(
    font: ("Arial"),
    size: 11.5pt,
    lang: "it",
    fill: get-light-background(),
  )

  set par(justify: true, linebreaks: "optimized")
  set list(indent: 1.2em, tight: false)
  set enum(indent: 1.2em, tight: false)
  set heading(numbering: "1.1")
  // show math.equation: set text(font: "Fira Math")

  // title page
  align(
    left + horizon,
    {
      v(2.5cm)

      text(
        size: 2.7em,
        weight: "bold",
        title.replace("@", "\n@"),
      )

      parbreak()

      text(
        size: 1.2em,
        subtitle,
      )

      v(0.5cm)

      let author_display(fullname, mail, github) = align(
        center,
        {
          set text(size: 1.1em)
          strong(smallcaps(fullname)) + linebreak()
          link("mailto:" + mail, raw(mail)) + linebreak()
          text(fill: get-variables(), link(github, github))
        },
      )

      // authors
      grid(
        columns: (1fr,) * authors.len(),
        align: center,
        ..authors.map(author => (
          author_display(
            author.fullname,
            author.mail,
            author.github,
          )
        ))
      )

      v(3em)

      // tech stack
      if (tech-stack) {
        let logos = (
          image(width: 3cm, "img/java.svg"),
          image(width: 3cm, "img/thymeleaf.svg"),
          image(width: 3cm, "img/javascript.svg"),
        )
        grid(
          columns: (1fr,) * logos.len(),
          align: center + horizon,
          gutter: 1em,
          ..logos
        )
      }
    },
  )

  set page(
    numbering: "1",
    header: {
      set text(6 / 7 * 1em, font: ("Arial"))
      context {
        let sizeof(it) = measure(it).width
        // Queries the heading FOR THE PREVIOUS PAGE
        let headings1 = query(selector(heading.where(level: 1))).filter(h1 => here().page() - 1 == h1.location().page())
        let before = query(selector(heading.where(level: 1)).before(here()))

        let output

        output = if (headings1.len() != 0) {
          // if there is a lvl 1 heading on the page before, the header must be empty
          none
        } else if (before.len() != 0) {
          // otherwise it's the name of the current lvl 1 heading
          let numbering = counter(heading.where(level: 1)).display().first()
          text(
            // fill: light-background,
            {
              numbering
              [ --- ]
              before.last().body
            },
          )
        }

        if (calc.even(here().page())) {
          counter(page).display()
          h(1fr)
          title
        } else {
          output
          h(1fr)
          counter(page).display()
        }
        // v(-0.5em)
        // line(length: 100%, stroke: 0.1pt + black)
      }
    },
    header-ascent: 40%,
  )

  // mainmatter
  show outline.entry: it => {
    v(1em, weak: true)
    link(
      it.element.location(),
      it.indented(
        it.prefix(),
        it.element.body + box(width: 1fr, repeat([\u{0009} . \u{0009} \u{0009}])) + it.page(),
      ),
    )
  }

  show outline.entry.where(level: 1): it => {
    v(1.6em, weak: true)
    link(
      it.element.location(),
      strong(
        it.indented(
          it.prefix(),
          it.element.body + h(1fr) + it.page(),
        ),
      ),
    )
  }

  set page(columns: columns)
  outline()

  show raw.where(block: true): it => {
    // set text(font: "JetBrains Mono NF", weight: "light")
    set text(size: 0.9em)
    block(
      width: 100%,
      fill: rgb("#ebf1f5"),
      inset: 10pt,
      stroke: rgb("#9cc9e7"),
      // radius: 4pt,
      it,
    )
  }

  show ref: it => text(fill: get-functions(), it)
  show link: it => text(fill: blue, underline(stroke: blue, it))

  set figure(gap: 1.5em)

  body
}

// LEGEND
#let legend(arr) = {
  let num = int(arr.len() / 2)
  grid(
    columns: (auto,) + (1fr,) * num,
    align: center + horizon,
    stroke: silver,
    inset: 10pt,
    grid.cell(
      rowspan: num,
      rotate(
        -90deg,
        reflow: true,
        text(weight: "bold", style: "italic", smallcaps("Legend")),
      ),
    ),
    ..arr.flatten()
  )
}

#let SINGLE_COLUMN(body) = {
  set page(columns: 1)

  body
}

#let frontmatter(body) = {
  set page(columns: 1)
  set heading(numbering: none, outlined: false, bookmarked: true)

  show heading: it => {
    text(size: 1.2em, it)
    v(6pt)
  }

  body
}

#let mainmatter(body) = {
  set page(columns: 1)
  set heading(numbering: "1.1", outlined: true)

  show heading: it => {
    set text(font: ("Arial"))
    box(
      width: 1fr,
      inset: 8pt,
      fill: rgb("#254a85"),
      stroke: black,
      align(
        left,
        text(
          fill: get-selection-background(),
          smallcaps(counter(heading).display() + " " + it.body),
        ),
      ),
    )
  }

  show heading.where(level: 1): it => context {
    set page(header: { }, columns: 1)
    let number = if it.numbering != none { counter(heading.where(level: 1)).display() }
    set text(size: 1.80em, font: ("Arial"), hyphenate: false)
    set par(justify: false)
    v(30%)
    align(
      center,
      block(
        width: page.width * 0.61,
        smallcaps(number + v(0.7em) + it.body),
      ),
    )
  }

  body
}

#let appendix(body) = {
  set heading(numbering: "A.1")
  counter(heading).update(0)

  show heading.where(level: 1): it => context {
    set page(header: { }, columns: 1)
    let number = if it.numbering != none { counter(heading).display() }
    set text(size: 1.75em, font: ("Arial"), hyphenate: false)
    v(30%)
    align(
      center,
      block(
        width: page.width * 0.61,
        smallcaps(number + v(0.7em) + it.body),
      ),
    )
  }

  body
}

#let backmatter(body) = {
  body
}

#let thymeleaf_trick(body) = {
  show "thymeleaf": (
    text(fill: rgb("#005F0F"), "thymeleaf")
      + h(0.1cm)
      + box(
        image(
          "img/logos/thymeleaf.svg",
          width: 0.9em,
          height: 0.9em,
        ),
        // width: 1em,
        // height: 1em,
        baseline: 1pt
      )
  )

  show "Thymeleaf": text(fill: rgb("#005F0F"), "Thymeleaf")

  body
}

#let double_col_spaces(v_space) = {
  place(
    top,
    scope: "parent",
    float: true,
    v(v_space),
  )
}

// DATABASE
#let entity(string) = text(fill: red, weight: "semibold", string)
#let attr(string) = text(fill: olive, weight: "semibold", string)
#let attr_spec(string) = text(fill: olive, style: "oblique", string)
#let rel(string) = text(fill: blue, weight: "semibold", string)

// BEHAVIOURS
#let user_action(string) = text(fill: orange, weight: "semibold", string)
#let element(string) = text(fill: aqua.darken(40%), weight: "semibold", string)
#let page(string) = text(fill: purple, weight: "semibold", string)
#let server_action(string) = text(fill: yellow.darken(20%), weight: "semibold", string)

// PIPELINE
#let addon = emoji.rocket
#let u_action = emoji.person
#let s_action = emoji.computer
#let pg = emoji.page

#let comment(string) = text(fill: black.lighten(40%), h(1em) + raw("// " + string))

// Sequence diagrams

#import "@preview/chronos:0.2.1": * // sequence diagrams

#let thymeleaf = image("img/logos/thymeleaf.svg", width: 1.5em, height: 1.5em, fit: "contain")

#let balance(content, position: top) = context {
  let height = measure(
    // width: page.width - page.margin.left.length - page.margin.left.length,
    width: 21cm - 1.5cm - 1.5cm,
    content,
  ).height
  place(
    position,
    scope: "parent",
    float: true,
    block(
      height: 1.05 * height, // to fix calculation error
      columns(2, content),
    ),
  )
}

#let seq_diagram(
  title,
  diagram-code,
  label_: "",
  comment: "",
  comment_next_page_: true,
  next_page: true,
  add_comment: false,
  position: top,
) = context {
  place(
    position,
    scope: "parent",
    float: true,
    [
      #heading(level: 2, title)
      #label(label_)
    ],
  )
  [
    #figure(
      scope: "parent",
      placement: position,
      diagram-code,
    )
  ]
  if (add_comment) {
    place(
      position,
      scope: "parent",
      float: true,
      grid(
        columns: (auto, 1fr),
        column-gutter: 1em,
        align: center + horizon,
        text(
          weight: "bold",
          style: "oblique",
        ),
        line(
          length: 100%,
          stroke: 0.5pt + get-variables(),
        ),
      ),
    )
  }
  // [ --- ]
  balance(emph(comment), position: position)
  if (next_page) {
    pagebreak(weak: false)
  }
}

#let redirects = $-->$

#import "@preview/subpar:0.2.2": *

#let table-styles(body) = context {
  let frame(color) = (
    (x, y) => (
      left: if x > 0 {
        0pt
      } else {
        color
      },
      right: color,
      top: if y == 0 or y == 2 {
        color
      } else {
        0pt
      },
      bottom: color,
    )
  )

  let shading(dark, light, header) = (
    (x, y) => {
      if (y >= 2) {
        if calc.even(y) {
          dark
        } else if calc.odd(y) {
          light
        }
      } else {
        header
      }
    }
  )

  let custom-inset() = (
    (x,y) => {
      if (y<=2) {
        0.9em
      } else {
        0.6em
      }
    }
  )

  set table(
    stroke: frame(get-data-types()),
    fill: shading(
      get-selection-background(),
      get-default-background(),
      get-light-background()
    ),
    inset: custom-inset(),
    align: horizon
  )

  show table.cell: it => context {
    if (it.y <= 1) {
      set text(weight: "bold", size: 1.2em, style: "oblique", fill: get-selection-background())
      smallcaps(it)
    } else {
      it
    }
  }

  show table:it => {
    set par(justify: false)
    it
  }

  body
}

// CSS

#let css_explanation(css_source_code, comment) = context {
  table(
    inset: 10pt,
    stroke: (left: get-data-types() + 2pt, rest: none),
    {
      css_source_code
      emph(comment)
    },
  )
}

// Logos and such

// https://gist.github.com/felsenhower/a975c137732e20273f47a117e0da3fd1
#let LaTeX = {
  set text(font: "New Computer Modern")
  let A = (
    offset: (
      x: -0.33em,
      y: -0.3em,
    ),
    size: 0.7em,
  )
  let T = (
    x_offset: -0.12em,
  )
  let E = (
    x_offset: -0.2em,
    y_offset: 0.23em,
    size: 1em,
  )
  let X = (
    x_offset: -0.1em,
  )
  [L#h(A.offset.x)#text(size: A.size, baseline: A.offset.y)[A]#h(T.x_offset)T#h(E.x_offset)#text(size: E.size, baseline: E.y_offset)[E]#h(X.x_offset)X]
}

#let TeX = {
  set text(font: "New Computer Modern")
  let T = (
    x_offset: -0.12em,
  )
  let E = (
    x_offset: -0.2em,
    y_offset: 0.23em,
    size: 1em,
  )
  let X = (
    x_offset: -0.1em,
  )
  [#h(T.x_offset)T#h(E.x_offset)#text(size: E.size, baseline: E.y_offset)[E]#h(X.x_offset)X]
}

#let sqlite(string: "SQLite") = {
  set text(fill: gradient.linear(rgb("#0F80CC"), rgb("#81CCF2")), weight: "bold")
  box(string)
}

#let ria() = {
  box(image("img/logos/javascript.svg"), height: 0.9em, baseline: 1pt)
}