# stroopwafel-tutorial — Handoff Brief

**For:** a fresh Claude instance (or any collaborator) starting the
tutorial cold.
**Purpose:** what to build, what not to build, and the constraints you
need to know before you start teaching.
**Date written:** 2026-04-11, by Claude Opus 4.6 at the end of a long
session that built the SPKI/SDSI assertion model end-to-end. I'm too
close to the material to write the tutorial well — I know too much
to feel what a beginner feels. You read this brief cold and teach
better than I could.

---

## 1. What stroopwafel is (beginner-framed — use this framing, not the expert one)

Stroopwafel is a Clojure library for **deciding whether to allow or
deny something**. The decision is based on facts — who you are, what
you're asking for, what you've been granted — combined via logic.

That's it. At the core, one function: `evaluate`. Facts in, decision
out.

The library doesn't know about HTTP, databases, capabilities, OAuth,
or trust models. It just pattern-matches facts against rules and
tells you whether a query succeeded. Everything else — crypto,
signed tokens, delegation, revocation — is built in *separate
libraries* that feed facts into stroopwafel. This separation is
the whole point: stroopwafel stays tiny and universal; the
interesting politics of "who gets to say what" live in other layers.

**Do not open the tutorial by explaining:**
- SPKI/SDSI (a '90s capability research project most people haven't
  heard of)
- Biscuit (a capability token format from the mid-2010s)
- Capability-based security (too abstract, too soon)
- Datalog (yes it's the engine, but don't lead with the theory)
- Trust roots, delegation, revocation, attenuation (all important,
  all late in the tutorial, none of them in Chapter 1)

**Do open the tutorial by:**
- A concrete problem: "you have a service and you need to decide
  whether to let someone do a thing"
- A trivial example: three facts, one rule, one query, one
  decision. Maybe ten lines of Clojure.
- Showing that the engine doesn't care what the facts *mean* — it
  just matches patterns. The reader's intuition should be "huh,
  this is a tiny pattern-matching machine, I could use this for
  lots of things."

The sophisticated vocabulary (capability, SPKI, delegation, etc.)
enters the tutorial *only after the reader already has working
examples*. You introduce the word when the reader is ready to attach
it to something they already understand — not the other way around.

---

## 2. The three libraries and what each one does

Stroopwafel lives at the bottom of a three-library stack. A complete
authorization story uses all three, but the tutorial should
introduce them in order, each with its own working example before
the next is mentioned.

```
┌──────────────────────────────────────────────────────────┐
│  stroopwafel          — pure Datalog engine              │
│  zero dependencies, one function: evaluate               │
│  facts in, decision out, nothing else                    │
└──────────────────┬───────────────────────────────────────┘
                   │ used by
┌──────────────────▼───────────────────────────────────────┐
│  signet             — Ed25519/X25519 crypto primitives   │
│  sign-edn, verify-edn, keypair, kid URNs, SSH import     │
│  used for cryptographically signed facts and tokens      │
└──────────────────┬───────────────────────────────────────┘
                   │ used by
┌──────────────────▼───────────────────────────────────────┐
│  stroopwafel-pdp    — the PDP bridge                     │
│  Two PDPs actually:                                      │
│    stroopwafel.pdp.*       — Biscuit-shaped chain PDP    │
│    stroopwafel.pdp.spki.*  — SPKI/SDSI assertion PDP     │
│  Verifies signatures, extracts facts, calls evaluate     │
└──────────────────────────────────────────────────────────┘
```

**Pedagogical implication:** the tutorial's first four or five
chapters should use ONLY stroopwafel. Unsigned facts. Hand-written.
No crypto, no signatures, no tokens, no PDP. The reader learns what
facts, rules, checks, and policies are before they ever see a
signature. This is counterintuitive because "real" auth always has
signatures — but the signatures are a distraction from the logic.
Teach the logic first.

Only when the reader can confidently read and write stroopwafel
policies does the tutorial introduce signet (Chapter 5? 6?),
explaining "now we want the facts to be cryptographically
attributable" and demonstrating how `signet.sign/sign-edn` wraps
a payload, how verification produces a `:signer` kid, and how you
inject that kid into the stroopwafel fact store.

Then — much later — stroopwafel-pdp enters as "here's what you do
when you want the verification + fact-extraction + evaluation
pipeline wrapped up nicely." By then the reader knows what each
step is doing and why.

---

## 3. Recommended tool: Clay (scicloj) — JVM only for rendering

After surveying Clerk, Clay, and Jupyter, Clay is the right choice
for this tutorial.

**Important finding from the scaffolding session:** Clay **does not
run under babashka** in v2.0.15. Every entry namespace (api, make,
notebook, read) imports a JVM-only class transitively:

- `nextjournal.beholder` (file watcher for live-reload) imports
  `io.methvin.watcher.DirectoryChangeEvent` — not in bb's class list
- `scicloj.kind-portal` (image support via item.clj) needs
  `java.awt.image.BufferedImage` — bb doesn't ship AWT
- `parcera.antlr.java` (source parser via read.clj) uses ANTLR
  Java interop — also not in bb

**What this means for the tutorial workflow:**

- Notebook SOURCE is plain `.clj` files with Clay's comment/ns
  conventions. These run fine under bb if you just want to try
  snippets (`bb -e ...`) — the stroopwafel libraries themselves
  are bb-compatible.
- Notebook RENDERING requires JVM Clojure. The `bb.edn` in this
  repo provides `bb render`, `bb serve`, `bb render-all` as bb
  task shortcuts, but they all shell out to `clojure` internally
  (see `bb.edn :tasks`).
- Readers who want to run the tutorial need JVM Clojure + the
  Clay dep. A bb-only reader cannot render the tutorial; they
  can only run individual snippets from the source files.

Short version:

- **Clay** is a Clojure-native notebook renderer from the scicloj
  community. You write a `.clj` file with prose comments between
  forms; Clay evaluates and renders to HTML, Quarto, or Jupyter
  output. One source, multiple targets.
- **Clerk** is the alternative — also Clojure-native, very similar
  feel, slightly more mature, Clojure-only output. Good second
  choice if Clay trips you up.
- **Jupyter** (via clojupyter) is a bad fit for this specific
  tutorial. The cell-execution-order footgun is pedagogically
  hostile to beginners, the kernel is fragile, and the `.ipynb`
  JSON format is git-unfriendly.

Why Clay specifically:
1. **Quarto output path.** If the tutorial gets any readership,
   you want to ship it as a polished static site with proper
   navigation, searchable content, and print-friendly formatting.
   Clay → Quarto is the clean path to that.
2. **Git-friendly.** Plain `.clj` source files review like normal
   code.
3. **Multi-target.** Same source renders to HTML (for the website),
   Jupyter (for readers who want to play with it in their own
   environment), or GitHub-flavored markdown (for quick preview).
4. **Real Clojure, no cell boundaries.** Readers who copy-paste
   into a REPL get code that actually runs.

If Clay turns out to be too new or too rough, fall back to Clerk
without debate. Don't spend more than a day evaluating.

---

## 4. Scaffolding: what's already in place

The tutorial repo is **already scaffolded and verified working** as
of the session that wrote this file. You don't need to set up the
environment — just read HANDOFF.md, run `bb smoke` to confirm the
libraries load, and start writing chapter content.

### Files in the repo

```
stroopwafel-tutorial/
├── HANDOFF.md                    ← this file
├── README.md                     ← stub pointing readers here
├── LICENSE
├── deps.edn                      ← JVM Clojure + Clay + three libs
├── bb.edn                        ← bb task shortcuts
├── notebooks/
│   └── 01_first_decision.clj     ← Chapter 1 stub (replace with real content)
└── src/                          ← helper code lives here if needed
```

### deps.edn (JVM Clojure side — Clay rendering)

```clojure
{:paths ["src" "notebooks"]
 :deps  {org.clojure/clojure                  {:mvn/version "1.12.4"}
         ;; Three-library auth stack — installed to ~/.m2 via the
         ;; Path D workflow; :mvn/version coords work today and
         ;; will work transparently when these are on Clojars.
         com.github.franks42/stroopwafel      {:mvn/version "0.11.0"}
         com.github.franks42/signet           {:mvn/version "0.3.1"}
         com.github.franks42/stroopwafel-pdp  {:mvn/version "0.2.0"}
         ;; Notebook renderer (JVM-only, see §3)
         org.scicloj/clay                     {:mvn/version "2.0.15"}}

 :aliases
 {:test {:extra-paths ["test"]
         :extra-deps {io.github.cognitect-labs/test-runner {:git/tag "v0.5.1" :git/sha "dfb30dd"}}
         :main-opts ["-m" "cognitect.test-runner"]
         :exec-fn cognitect.test-runner.api/test}

  ;; Escape hatch: override the three libs with :local/root source
  ;; trees for cross-repo debugging. Run: clojure -M:dev:repl
  :dev  {:override-deps
         {com.github.franks42/stroopwafel     {:local/root "../stroopwafel"}
          com.github.franks42/signet          {:local/root "../signet"}
          com.github.franks42/stroopwafel-pdp {:local/root "../stroopwafel-pdp"}}}}}
```

### bb.edn (babashka side — task orchestration)

```clojure
{:paths ["src" "notebooks"]
 :deps  {com.github.franks42/stroopwafel     {:mvn/version "0.11.0"}
         com.github.franks42/signet          {:mvn/version "0.3.1"}
         com.github.franks42/stroopwafel-pdp {:mvn/version "0.2.0"}}
 ;; Note: Clay is NOT in bb's :deps — it doesn't load under bb
 ;; (see §3). bb tasks that touch Clay shell out to `clojure -M`.

 :tasks {...}}
```

Task shortcuts available via `bb <task>`:

| Task | What it does |
|---|---|
| `bb smoke` | 3-library sanity check: load stroopwafel/signet/spki, run one evaluate, sign one envelope, run one SPKI decide. Fast, no JVM spin-up. First thing to run when starting work. |
| `bb render <f>` | Render one notebook file to HTML via Clay. Shells out to `clojure -M`. Writes to `docs/`. |
| `bb serve` | Clay live-reload dev server — edit a notebook, save, browser auto-refreshes. The primary inner loop for HUMAN authors. |
| `bb render-all` | Render every `notebooks/*.clj` to HTML. |
| `bb preview` | Serve `docs/` on http://localhost:8765 via `python3 -m http.server` (foreground). For AI/MCP inner loops where `file://` URLs are blocked. |
| `bb preview:bg` | Same, but in the background. Writes `.preview.pid`. |
| `bb preview:stop` | Stop the background preview server. |
| `bb lint` | clj-kondo over `notebooks` + `src`. |
| `bb fmt` / `bb fmt:fix` | cljfmt check / auto-fix. |

### notebooks/01_first_decision.clj

A **placeholder** Chapter 1 that demonstrates:
- Clay's notebook conventions (prose as `;;` comments between code forms)
- Correct `ns` + `:require` for the tutorial
- Minimum `stroopwafel.core/evaluate` example
- Both positive and negative decisions

**This file has a "note to the author" at the top explicitly
asking you to rewrite it before shipping.** It's a live scaffold,
not finished content. Use it as a template for chapter structure,
then replace it with your real Chapter 1.

### Verified end-to-end (as of session that wrote this)

- `clojure -Stree` resolves the full dep tree from `~/.m2` cleanly
- Clay v2.0.15 downloaded + cached on first resolution
- `bb smoke` → 3/3 green
- Chapter 1 stub loads and runs in both bb and JVM Clojure

### Important non-goal

The tutorial should not require readers to clone signet,
stroopwafel, or stroopwafel-pdp separately. Assume `:mvn/version`
works. If publication to Clojars is still pending when readers
arrive, the README can add a one-paragraph "for now you need to
install these locally" workaround pointing at each library's
`build.clj`, but the design assumes a clean Clojars experience.

---

## 4.1 Browser-automation inner loop (for Claude instances)

If you're an AI instance (Claude or otherwise) working on this
tutorial, you need a way to actually SEE the rendered HTML — not
just read the `.clj` source or the raw HTML text. Visual layout,
heading hierarchy, code-block rendering, and result-block
placement are part of what the tutorial is, and you can't judge
them by reading the source.

**The working recipe, verified in the session that wrote this:**

1. **Render to HTML** — `bb render notebooks/<your-file>.clj`
   writes output to `docs/<file-basename>.html`.

2. **Start the preview server** — `bb preview:bg` starts a local
   HTTP server on port 8765 serving the `docs/` directory. The
   `file://` protocol is blocked by the Playwright MCP for
   security reasons, so an HTTP surface is required.

3. **Navigate via Playwright MCP** — load the Playwright MCP
   tools if they're not already available
   (`ToolSearch` with query
   `"select:mcp__playwright__browser_navigate,mcp__playwright__browser_snapshot,mcp__playwright__browser_take_screenshot,mcp__playwright__browser_close"`),
   then call `browser_navigate` with
   `http://localhost:8765/<file-basename>.html`.

4. **Get an accessibility snapshot** — call `browser_snapshot`.
   This returns a structured YAML tree of the rendered page
   (headings, paragraphs, code blocks, etc.) which is more useful
   than a pixel screenshot for most tutorial-editing purposes —
   you can read it, grep it, and reason about it.

5. **Optional pixel screenshot** — call `browser_take_screenshot`
   with `fullPage: true` for a visual check when layout matters
   (e.g., "does the code block CSS look right?"). The screenshot
   is saved to a file you can view.

6. **Close the browser** — `browser_close` when done.

7. **Stop the preview server** — `bb preview:stop`.

**Alternative: Chrome DevTools MCP** — if available, the
`mcp__chrome-devtools__*` family works similarly and gives you
real Chrome rendering (better visual fidelity, access to devtools
panels). Playwright is simpler for the "does the render look
right?" inner loop; Chrome DevTools is better when you need to
inspect network requests, console errors, or computed CSS.

**What the verification session confirmed:**

- `bb render notebooks/01_first_decision.clj` produces a
  717-line HTML file at `docs/first_decision.html`.
- `bb preview:bg` + `curl -s -o /dev/null -w "%{http_code}"
  http://localhost:8765/first_decision.html` returns `200`.
- Playwright `browser_navigate` + `browser_snapshot` returns a
  structured YAML tree showing all Clay rendering elements:
  headings ("Chapter 1 — Your first authorization decision",
  "The three ingredients", "Facts", "A decision", "What's next"),
  prose paragraphs, code blocks with the actual `(sw/evaluate
  ...)` forms, and result blocks displaying `{:valid? true}` and
  `{:valid? false}` from the real library calls executed at
  render time.
- Full-page screenshot renders correctly at page scale.

**Workflow tip for iterating on a chapter:**

```
bb render notebooks/02_multiple_facts.clj    # rebuild HTML
# navigate Playwright → snapshot → read the structure
# if something looks wrong, edit the .clj source
bb render notebooks/02_multiple_facts.clj    # rebuild
# navigate Playwright → snapshot → verify fix
```

The `bb serve` task (Clay's live-reload server) is the
equivalent for human authors — edit, save, browser auto-refreshes.
Don't use `bb serve` for Claude-driven work; it's harder to
script than the explicit render → navigate → snapshot loop.

---

## 5. Suggested pedagogical arc (subject to your judgment)

Rough chapter progression. You'll want to adjust — this is a
sketch, not a contract.

**Chapter 0 — Welcome**
What this tutorial is, who it's for, what you'll build. Keep it
short. Mention "we're going to learn a tiny authorization engine
and grow it into something surprisingly powerful."

**Chapter 1 — Facts, rules, decisions**
Pure `stroopwafel.core/evaluate`. Trivial facts like
`[:user "alice"]`. One rule. One policy. One `:allowed?` decision.
The reader types ten lines and sees it work. No crypto, no
signatures, no tokens.

**Chapter 2 — Multiple facts, joining predicates**
Introduce Datalog's pattern-matching intuition through examples.
Rules that join across facts. Show how a rule *derives* new facts.
Maybe a tiny RBAC example: "users have roles, roles have
permissions, a user is allowed if a role of theirs has the
permission." Still no crypto.

**Chapter 3 — Checks and policies**
Explain the difference between a check (must pass or the whole
thing fails) and a policy (ordered, first-match wins). This is
where the reader starts to see stroopwafel's real shape. Build
a small working example with both. Show an explain trace.

**Chapter 4 — Blocks and scope**
Introduce the idea that facts can be grouped into blocks, and that
different blocks have different *visibility*. The authorizer block
vs. the block-local scope. This is where Biscuit-style attenuation
semantics start to peek through, but you don't need the word
"Biscuit" yet.

**Chapter 5 — Signed facts (signet enters)**
Now we motivate crypto. "What if the fact came from someone we
needed to trust?" Introduce `signet.key/signing-keypair`,
`signet.sign/sign-edn`, `verify-edn`. Show how a signed envelope
carries a `:signer` kid. Start injecting signed facts into
stroopwafel manually — extract the signer kid, verify, add as
a fact, evaluate.

**Chapter 6 — The PDP pattern (stroopwafel-pdp enters)**
The manual signer-extraction in Chapter 5 gets tedious. Introduce
`stroopwafel.pdp.core` as the convenience layer: `context`,
`add-chain`, `add-signed-envelope`, `add-facts`, `decide`. Show
how the Biscuit-style chain token works: one root authority,
blocks extending, sealing at the end, verification. This is the
first "real token" in the tutorial.

**Chapter 7 — SPKI/SDSI assertions (the other model)**
The second PDP. `stroopwafel.pdp.spki.*`. Shift perspective: "what
if instead of one chain, we have independent signed statements
from different parties?" Introduce name-binding and capability
assertions. Show the SDSI-style name resolution example: "Alice is
in ops-team, ops-team can restart /api, therefore Alice can
restart /api." This is the payoff chapter — it's where the reader
sees that the stroopwafel foundation supports genuinely different
trust models with very little additional machinery.

**Chapter 8 — Trust, delegation, attenuation**
By now the reader has seen two trust models. Introduce delegation:
signing a statement that grants someone else the right to issue
certain kinds of statements. Walk through the trust-closure
fixpoint. Show attenuation: how narrowing happens automatically.

**Chapter 9 — Revocation**
Three scopes: by id, by key, by subject. Show how revocation
participates in the same Datalog engine rather than being a
separate subsystem. The reader realizes everything is facts all
the way down.

**Chapter 10 — Pulling it together**
A small end-to-end example. Maybe a tiny HTTP service (Ring
handler) where a client presents a signed token and the service
authorizes via stroopwafel-pdp. Doesn't have to be polished —
just "here's what it looks like in a real-ish context."

**Chapter 11 — Where to go next**
Links to the full libraries, the design docs, the alpaca-clj
reference implementation (which is a production-shaped
capability-gated trading proxy). Mention that every concept in
this tutorial is used in a real paper-trading-backed system.

Total: ~10-12 chapters. The tutorial should be the kind of thing a
curious Clojure developer can work through in a long afternoon and
come away with a working mental model plus a half-dozen toy
examples they can modify.

---

## 6. Reference materials (where to find code and docs)

All on this machine:

- **`../stroopwafel/`** — the engine. Read `src/stroopwafel/core.clj`
  first. It's ~150 lines and the comments are reasonable. The
  `test/stroopwafel/core_test.clj` has many short examples.
- **`../signet/`** — the crypto library. Read `src/signet/key.cljc`,
  `src/signet/sign.cljc`, `src/signet/chain.cljc` in that order.
  The key concept: kid URNs (`urn:signet:pk:ed25519:<base64url>`)
  as self-describing identities.
- **`../stroopwafel-pdp/`** — the bridge. Two parallel trees:
  `src/stroopwafel/pdp/{core,trust,replay}.clj` (chain PDP) and
  `src/stroopwafel/pdp/spki/{core,assertion,trust,templates,revocation}.clj`
  (assertion PDP). Read `stroopwafel.pdp.core` first for the
  simpler shape, then `stroopwafel.pdp.spki.core` for the
  more recent design.
- **`../stroopwafel-pdp/docs/assertion-api-proposal.md`** — the
  SPKI/SDSI design document. Long, but the table of contents is
  navigable. Useful reference when writing Chapter 7. Has explicit
  "pinned decisions" explaining *why* the library looks the way it
  does — save the tutorial reader from re-deriving everything.
- **`../stroopwafel/docs/generic-assertion-schema.md`** and its
  reviews — the original proposal that led to
  `stroopwafel.pdp.spki`. Read these ONLY if you want deep
  background on why the assertion model exists; the tutorial
  shouldn't reference them directly.
- **`../alpaca-clj/`** — a production-shaped consumer. A
  capability-gated proxy for Alpaca paper-trading, running four
  auth modes in parallel (bearer / bound / group / spki). Read
  `src/alpaca/auth.clj` for a real-world integration example. 98
  tests, all green. Version 1.0.0 as of this session.

Testing conventions across the repos:
- Tests live in `test/...` paths matching `src/...`.
- JVM test runner: `clojure -M:test`
- Babashka (where supported): `bb smoke` for signet, `bb test` for
  alpaca-clj.

---

## 7. What to assume about the reader

- **Knows Clojure.** The tutorial is in Clojure; assume
  comfort with sequences, maps, keywords, namespaces, `require`,
  `def`. You do NOT need to explain destructuring or threading.
- **Does NOT know Datalog.** Introduce the pattern-matching
  intuition with examples before you use the word "Datalog."
- **Does NOT know capability-based security.** In fact, many
  readers will arrive knowing RBAC (roles/permissions) or
  OAuth-style bearer tokens. Use those as points of comparison
  late in the tutorial, when you want to contrast: "notice that
  stroopwafel is more like a small Prolog than like OAuth."
- **Does NOT know SPKI/SDSI.** Do not define the acronyms until
  Chapter 7 at the earliest, and even then only as "this
  particular style of authorization is called SPKI/SDSI in
  the academic literature from the '90s" — don't make the
  reader care about the history.
- **Does NOT want to read a research paper.** The tutorial is
  about *doing*, not about reading a decade of capability-system
  literature. If you catch yourself writing a paragraph that
  sounds like a survey article, delete it and replace with a
  working example.
- **May want to adapt the ideas for their own domain.** The
  tutorial examples should be varied enough to suggest that
  stroopwafel is a general tool. Don't make every example a
  bank transfer or a file permission; mix in things like
  "can this agent run this workflow" or "can this caller
  access this column."

---

## 8. Concrete non-goals

- **Do NOT build a library yourself.** The tutorial is docs, not
  code. The three libraries exist; use them.
- **Do NOT try to be comprehensive.** A tutorial that covers 60% of
  stroopwafel well is much more valuable than one that covers 100%
  badly. When in doubt, cut.
- **Do NOT introduce HTTP, servers, or web frameworks until
  Chapter 10 at the earliest.** The whole point of starting with
  `evaluate` is that the engine is free of context — don't bury it
  in Ring middleware.
- **Do NOT explain the internal implementation of stroopwafel.**
  No "here's how the Datalog fixpoint works" or "here's how
  unification is implemented." The tutorial is about *using*
  stroopwafel, not *being* stroopwafel.
- **Do NOT reference this handoff doc in the tutorial.** The
  reader doesn't need to know about the meta-conversation that
  produced it. Delete or move this file once you've absorbed it.
- **Do NOT refactor the libraries to make the tutorial easier.**
  If you find something awkward in the API, note it as
  feedback to discuss with Frank, but don't touch the library
  code from the tutorial project.

---

## 9. Opening move for a fresh instance

If you're picking this up cold, here's what I'd do in the first
hour:

1. **Read this file once end to end.** (~20 min)

2. **Run `bb smoke` from the repo root.** Confirms the three
   libraries are resolvable via `~/.m2` and that
   stroopwafel/signet/spki all evaluate correctly. If this fails,
   stop and fix the environment before doing anything else —
   every step below assumes a working stack. (~1 min)

3. **Read the existing scaffold.** In order:
   - `deps.edn` and `bb.edn` — confirm you understand what's wired
     where. Should be obvious after §4 above.
   - `notebooks/01_first_decision.clj` — the Chapter 1 stub.
     Read it as a FORMAT EXAMPLE, not as content you'll keep. It
     shows Clay's notebook conventions and confirms the libs
     load. The actual content is placeholder. (~5 min)

4. **Read the stroopwafel engine.** `../stroopwafel/src/stroopwafel/core.clj`
   — the whole engine, ~150 lines, the comments are reasonable. (~10 min)

5. **Read three short tests.** Pick the three shortest tests in
   `../stroopwafel/test/stroopwafel/core_test.clj`, trace through
   what they do. The goal: feel how `evaluate` pattern-matches
   facts against queries. (~10 min)

6. **Try one live example in bb.** Open `bb` (or use `bb -e`) and
   type a tiny stroopwafel example — three facts, one policy,
   one `evaluate` call. Confirm it prints `{:valid? true}`. This
   is the same thing the reader will do in Chapter 1, and you
   need to feel the shape of that experience. (~5 min)

7. **Render the stub and look at it.** Run `bb render
   notebooks/01_first_decision.clj` (writes to
   `docs/first_decision.html`). If you're a human author, open
   that file in a browser. If you're a Claude instance, see §4.1
   for the full render → preview:bg → Playwright navigate →
   snapshot loop. Either way, confirm Clay renders your
   environment correctly — prose as markdown, code as Clojure,
   result blocks showing evaluated output. This is the reader's
   experience. (~5 min)

8. **Throw away the stub and write the real Chapter 1.** One
   page. Show it to Frank. Iterate. Don't start Chapter 2 until
   Chapter 1 feels right to both of you. (~5 min)

If you can't write Chapter 1 in one page after steps 4–6, you
haven't understood the engine yet — go back to step 5 and read
more tests.

---

## 10. Meta-notes from the session that produced this

Not strictly necessary for the tutorial work, but useful context:

- The SPKI/SDSI design doc + implementation happened in one long
  session (6 phases, ~2000 lines of code + tests, all green). The
  experience of writing that code taught me what's subtle and what
  isn't. The tutorial should capture the *subtleties* — the things
  I got wrong the first time — not just the happy path.
- Specific things I got wrong and had to fix:
  - `?_` is not a wildcard in stroopwafel's Datalog — repeated
    occurrences unify. The tutorial should introduce this as a
    gotcha when it gets to templates.
  - Signet had a bb-compat bug where `seed->public-key` used
    `proxy [SecureRandom]` which bb can't handle. If the tutorial
    demonstrates bb compatibility, be aware that older signet
    versions may break.
  - Temporal `:not-after` defaulting is subtle. After walking
    through four options, I landed on "default to infinity +
    lint warning." The tutorial should not re-litigate this;
    just explain the current behavior.
  - Trust closure with delegation is a fixpoint, and the
    termination guard matters. Not tutorial content unless you
    explicitly decide to go deep into delegation mechanics.

- The core insight the tutorial should drive home: **the engine
  doesn't know what the facts mean; trust, attenuation, revocation,
  and delegation are all just more facts.** Once a reader
  understands this, the rest falls into place. Before they
  understand this, nothing quite makes sense.

- Frank's stated audience: people who struggle with SPKI / SDSI /
  capabilities / Datalog / Biscuit. Meet them where they are.
  Frank is also explicitly aware that he (and I, the session
  Claude) are "library-brain-damaged" — too close to the material
  to see beginner confusion. Trust your fresh perspective.

Good luck. The library is good. It deserves a good tutorial.
