;; # Chapter 2 — But who said so?

(ns who-said-so)

;; In Chapter 1 we ran a real authorization decision:
;;
;; > **alice can edit posts — allowed.**
;;
;; Let's pause on that for a second. *Why* did we believe it?
;;
;; We typed `[:has-role "alice" :editor]` into a Clojure vector and
;; stroopwafel matched against it. The engine was happy. The query
;; succeeded. But if you squint, nothing about that fact is *true*
;; in any absolute sense. It's true because we wrote it down.
;;
;; In a real system you never just *know* things. You know them
;; because **someone told you**.

;; ## The world is full of sayers
;;
;; Think about where authorization facts actually come from in
;; systems you already use:
;;
;; - **LDAP / Active Directory.** A directory server says *"alice
;;   is in the `editors` group."* Your service believes the
;;   directory.
;; - **DNS + DNSSEC.** A resolver says *"`api.example.com` lives at
;;   `10.0.0.5`."* You believe it because the zone is signed by a
;;   key that chains back to a root you decided to trust.
;; - **X.509 certificate authorities.** A certificate says *"this
;;   TLS connection really belongs to `bank.com`."* You believe it
;;   because Mozilla shipped a root store on your laptop and you
;;   never second-guessed it.
;; - **OAuth issuers.** A JWT says `"sub": "alice"` and your API
;;   trusts it because the `iss` claim matches an issuer you
;;   whitelisted in config.
;; - **GitHub org membership.** GitHub says *"alice is a member of
;;   `acme-eng`."* Your CI believes GitHub because it authenticated
;;   to GitHub with a token.
;; - **Signed git commits.** A commit says *"alice wrote this."*
;;   You believe it because her public key is in your keyring.
;;
;; Each of these looks completely different on the wire. LDAP is a
;; binary protocol from 1993. DNSSEC is signed zone files. X.509 is
;; ASN.1 and a web of trust. OAuth is JSON in a Bearer header. Git
;; uses Ed25519 or RSA signatures. But the **shape** underneath is
;; the same every time. Four things, always:
;;
;; 1. **An asserter** — someone making a claim.
;; 2. **An assertion** — the claim itself.
;; 3. **A scope** — what this asserter is allowed to speak about,
;;    and when.
;; 4. **A trust decision** — ours, about whether we care what this
;;    asserter says, for this scope, right now.
;;
;; Take any of those four away and you don't have authorization.
;; You have a prayer.

;; ## Real-world messiness
;;
;; The moment you start writing sayers down — even informally —
;; things you hadn't thought about start showing up.
;;
;; **Whose alice?** LDAP has an `alice`. GitHub has an `alice`.
;; Payroll has an `alice`. Are they the same person? Probably not.
;; A name only means something *inside* the place that issued it.
;; "alice" without a sayer attached is almost meaningless.
;;
;; **Transitive trust.** Root trusts HR. HR trusts the team leads.
;; Team leads trust the engineers they onboard. When an engineer
;; asserts something, you're really trusting a chain — and any
;; weak link breaks the whole thing. Most systems can't even
;; *express* this, let alone reason about it.
;;
;; **Scoped trust.** You trust payroll to tell you *who is an
;; employee*. You do **not** trust payroll to tell you *who is an
;; admin*. Same asserter, two very different levels of credence
;; depending on what they're claiming. Most real systems get this
;; wrong by flattening trust to one dimension — "we trust LDAP" —
;; and then everyone with write access to LDAP can mint admins.
;;
;; **Revocation lag.** Alice left the company two weeks ago. HR
;; updated the directory yesterday. The signed token in her laptop
;; is still valid until next Tuesday. Which of the four sayers is
;; currently right about "alice is an employee"? More importantly:
;; which one does your service listen to?
;;
;; **Stale assertions.** The capability you were granted in 2019
;; is still sitting in a database somewhere, blessing an action
;; nobody remembers authorizing. Nothing *revoked* it; the sayer
;; just never got around to saying otherwise.
;;
;; None of this is solvable with a bigger policy file. It's only
;; solvable if the system *natively understands* that every fact
;; has a sayer, a scope, and a trust decision attached to it — and
;; can reason about all three at the same time.

;; ## That's what we're going to build
;;
;; From Chapter 3 onward, every fact in this tutorial is an
;; **assertion with a named sayer**. Trust itself is going to be a
;; fact too — something we write down, something we can revoke,
;; something rules can reason about just like any other data.
;;
;; Chapter 1's example rewritten with the mindset looks like this.
;; Don't try to run it yet — just read it and notice the shape.
;; It keeps the same labeled-vector convention we started using in
;; Chapter 1:
;;
;; ```clojure
;; (def assertions
;;   [[:asserts, :sayer :root, :subject "alice", :action :edit, :resource :posts]
;;    [:asserts, :sayer :root, :subject "bob",   :action :read, :resource :posts]])
;;
;; (def trust
;;   [[:trust, :sayer :root]])
;;
;; (def rules
;;   '[{:head [:can,     :subject ?s,     :action ?a, :resource ?r]
;;      :body [[:asserts, :sayer ?sayer,  :subject ?s,
;;                        :action ?a,     :resource ?r]
;;             [:trust,   :sayer ?sayer]]}])
;; ```
;;
;; Three things to notice:
;;
;; - Every assertion now carries an explicit **`:sayer`** slot —
;;   who is making the claim. In this toy example it's always
;;   `:root`, but later we'll see `:hr`, `:payroll`, `:github`,
;;   whatever the real world gives us.
;; - **Trust is just another fact.** `[:trust, :sayer :root]` is a
;;   vector in exactly the same way an assertion is. We could add
;;   `[:trust, :sayer :hr]` or remove trust entirely, and the
;;   decision machinery keeps working without changing a line.
;; - The rule only derives a usable `[:can ...]` fact when **a
;;   sayer we trust** has asserted it. The `?sayer` variable in
;;   the first body clause is the same `?sayer` in the second —
;;   stroopwafel will only find a match when *the same name*
;;   appears in both the assertion and the trust fact. Pull the
;;   `:trust` fact out and every decision in the system collapses
;;   to deny. Not by magic — by pattern matching, the same way
;;   Chapter 1 worked.
;;
;; That last point is the whole idea of this tutorial. Authorization
;; isn't a question about what is objectively *true* in the world.
;; It's a question about **what we're willing to act on, given who
;; is telling us, and how much we trust them for this kind of
;; claim**.
;;
;; Chapter 3 makes it concrete. We'll type the assertions in by
;; hand — still no crypto, still no signing — but the sayer slot
;; is in the data from here on, and the shape above is the shape
;; every decision in the rest of this tutorial will take.

^{:kind/hiccup true :kindly/hide-code true}
[:div {:style "margin-top:2em;padding-top:1em;border-top:1px solid #ddd;display:flex;justify-content:space-between;"}
 [:a {:href "first_decision.html"
      :style (str "display:inline-block;"
                  "padding:0.6em 1.2em;"
                  "background:#f3f4f6;"
                  "color:#0366d6;"
                  "text-decoration:none;"
                  "border:1px solid #d1d5db;"
                  "border-radius:4px;"
                  "font-family:sans-serif;"
                  "font-weight:500;")}
  "← Previous: Chapter 1"]
 [:a {:href "assertions_and_trust.html"
      :style (str "display:inline-block;"
                  "padding:0.6em 1.2em;"
                  "background:#0366d6;"
                  "color:white;"
                  "text-decoration:none;"
                  "border-radius:4px;"
                  "font-family:sans-serif;"
                  "font-weight:500;")}
  "Next: Chapter 3 — Making it real →"]]
