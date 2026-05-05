;; # Tutorial Sketch — The Assertion Model
;;
;; This is a pseudo-code sketch of the full tutorial rewritten
;; around the cleaner conceptual model we arrived at. Nothing here
;; evaluates yet — the `decide`, `assertion`, and `trust` functions
;; don't exist in the library. The goal is to see whether the
;; **pedagogical flow** feels right before committing to code.
;;
;; **The model in one sentence:** everything is an assertion from
;; a sayer. Trust is rooted in `:self` — the runtime making the
;; decision. The library's inference rules do the rest.

(ns new-model-sketch)

;; ---
;; ## The trust anchor: `:self`
;;
;; Every authorization decision is made by *someone* — a
;; service, a process, a running instance of your code. We call
;; that someone **`:self`**.
;;
;; `:self` is **local**. It's not a global authority, not a
;; shared root, not a network service. It's *this* process,
;; *this* PDP (policy decision point), *this* running instance
;; making *this* decision right now. Every PDP has its own
;; `:self`. Two services in the same organization each have
;; their own `:self`, each with their own configured trust
;; roots, each making independent decisions.
;;
;; `:self` is the one entity that is trusted unconditionally —
;; not because someone vouched for it, but because it IS the
;; thing doing the deciding. You can't make a trust decision
;; without trusting yourself to make it. That's not circular;
;; it's the starting point.
;;
;; `:self` is the **only true trust root** in the system.
;; Every other sayer — `:root`, `:hr`, `alice-kid`, the
;; revocation authority — is trusted only because *this*
;; `:self` asserted trust in them. They're trusted sayers,
;; not trust roots. The distinction matters: a trusted sayer's
;; authority is granted and can be revoked. `:self`'s trust in
;; itself is axiomatic — it can't be revoked because there's
;; nobody above it to do the revoking.
;;
;; A different service's `:self` might trust a completely
;; different set of sayers. That's fine — each PDP is
;; sovereign over its own decisions.
;;
;; The library provides one axiom:
;;
;; ```
;; [:trusted :self]
;; ```
;;
;; That's the single trust root. It's local to this PDP.
;; Everything else is assertions.

;; ---
;; ## Chapter 1 — Your first authorization decision
;;
;; *Opener: blog service, someone hits POST /posts/42/edit.*
;;
;; Three concepts, introduced in order:
;;
;; **1. An assertion** — a claim from a named sayer:
;;
;; ```clojure
;; (def a1
;;   (assertion {:sayer    :root
;;               :type     :capability
;;               :subject  "alice"
;;               :action   :edit
;;               :resource :posts}))
;; ```
;;
;; Read it like: *"root asserts that alice can edit posts."*
;; An assertion is always **from** someone (the `:sayer`) and
;; always **about** something (the `:type` + its fields).
;;
;; **2. Trust roots** — `:self` declares who it trusts:
;;
;; ```clojure
;; (decide {:trust      [:root]
;;          :assertions [a1]
;;          :query      [:can "alice" :edit :posts]})
;; ;; => {:allowed? true}
;; ```
;;
;; The `:trust [:root]` line is sugar. Under the hood it means:
;; `:self` asserts trust in `:root`. Since `:self` is the
;; unconditionally trusted trust anchor, this makes `:root`
;; trusted too. In a real deployment, these trust roots come
;; from configuration or are hard-coded into the service — either
;; way, they're anchored in the runtime itself.
;;
;; That's it. No rules to write, no blocks, no Datalog. Three
;; things in, one answer out.
;;
;; Flip the question:
;;
;; ```clojure
;; (decide {:trust      [:root]
;;          :assertions [a1]
;;          :query      [:can "bob" :edit :posts]})
;; ;; => {:allowed? false}
;; ```
;;
;; No assertion says bob can edit anything. Decision: deny.
;;
;; **What to notice:** the engine doesn't care what `:edit`,
;; `:posts`, or `"alice"` mean. It just checks whether a
;; sayer that `:self` trusts has asserted the capability
;; matching the query.

;; ---
;; ## Chapter 2 — But who said so?
;;
;; Same motivation chapter as the current draft. Pause on the
;; question: *why did we believe root?*
;;
;; Tour of real-world sayers (LDAP, DNS, X.509, OAuth, GitHub).
;; Four things every time: asserter, assertion, scope, trust
;; decision. Real-world messiness: whose alice, transitive trust,
;; scoped trust, revocation lag, stale assertions.
;;
;; **Punchline:** everything the user touches is an assertion —
;; including the trust decision itself. Every sayer is ultimately
;; trusted because `:self` — the runtime — said so. Trust roots
;; come from configuration or hard-coded setup, but they are
;; assertions by `:self` nonetheless.
;;
;; This chapter is prose-only. No new code.

;; ---
;; ## Chapter 3 — Trust is just another assertion
;;
;; Chapter 1 used `:trust [:root]` as if trust were magic. Now
;; we look under the hood.
;;
;; **`:trust [:root]` is syntactic sugar for an assertion by
;; `:self`:**
;;
;; ```clojure
;; ;; These are equivalent:
;; (decide {:trust [:root] ...})
;;
;; (decide {:assertions [(assertion {:sayer   :self
;;                                   :type    :trust
;;                                   :subject :root})
;;                       ...]
;;          ...})
;; ```
;;
;; `:self` is the runtime — the service making the decision.
;; It's the one entity that is unconditionally trusted (the
;; library provides `[:trusted :self]` as its single axiom).
;; When you write `:trust [:root]`, you're saying: "this
;; runtime asserts that it trusts `:root`."
;;
;; In a real deployment, trust roots come from somewhere
;; concrete — a config file, environment variables, hard-coded
;; constants in the service code. However they arrive, they
;; are assertions by `:self`. The runtime is the ultimate
;; authority on who it trusts, and it grounds that authority
;; in itself.
;;
;; **Pull trust out, decision collapses:**
;;
;; ```clojure
;; (decide {:trust      []          ;; :self trusts nobody
;;          :assertions [a1]
;;          :query      [:can "alice" :edit :posts]})
;; ;; => {:allowed? false}
;; ```
;;
;; Alice still has a perfectly good capability assertion from
;; root. But `:self` stopped trusting root, so the assertion
;; doesn't count. Authorization is not about what's "true" —
;; it's about what `:self` is **willing to act on**.
;;
;; **Multiple trust roots:**
;;
;; ```clojure
;; (def a-from-root
;;   (assertion {:sayer    :root
;;               :type     :capability
;;               :subject  "alice"
;;               :action   :edit
;;               :resource :posts}))
;;
;; (def a-from-hr
;;   (assertion {:sayer    :hr
;;               :type     :capability
;;               :subject  "alice"
;;               :action   :edit
;;               :resource :posts}))
;;
;; ;; :self trusts root but NOT hr:
;; (decide {:trust      [:root]
;;          :assertions [a-from-root a-from-hr]
;;          :query      [:can "alice" :edit :posts]})
;; ;; => {:allowed? true}
;; ;; (root's assertion counted; hr's was ignored because
;; ;;  :self never asserted trust in :hr)
;; ```
;;
;; **Multiple contexts, different trust roots:**
;;
;; A single runtime can make decisions in different contexts
;; with different trust roots:
;;
;; ```clojure
;; ;; Production context: trust the production root
;; (decide {:trust      [:prod-root]
;;          :assertions prod-assertions
;;          :query      [:can "alice" :deploy "/api"]})
;;
;; ;; Staging context: trust the staging root
;; (decide {:trust      [:staging-root]
;;          :assertions staging-assertions
;;          :query      [:can "alice" :deploy "/api"]})
;; ```
;;
;; Same `:self`, different trust decisions per context. The
;; trust roots are configured per environment, but they're all
;; anchored in `:self`.

;; ---
;; ## Chapter 4 — Names and groups
;;
;; New assertion type: `:name-binding`. A sayer binds a key (or
;; name) to a group name. This is SDSI's contribution.
;;
;; ```clojure
;; ;; Root says: ops-team can restart /api
;; (def cap
;;   (assertion {:sayer    :root
;;               :type     :capability
;;               :subject  "ops-team"
;;               :action   :restart
;;               :resource "/api"}))
;;
;; ;; Root says: alice's key is a member of ops-team
;; (def membership
;;   (assertion {:sayer    :root
;;               :type     :name-binding
;;               :key      alice-kid
;;               :name     "ops-team"}))
;;
;; (decide {:trust      [:root]
;;          :assertions [cap membership]
;;          :query      [:can alice-kid :restart "/api"]})
;; ;; => {:allowed? true}
;; ```
;;
;; The reader did NOT write a join rule. The library's inference
;; rules automatically connected the name-binding to the
;; capability: alice-key is named "ops-team", ops-team can
;; restart /api, therefore alice-key can restart /api.
;;
;; **Non-member case:**
;;
;; ```clojure
;; (decide {:trust      [:root]
;;          :assertions [cap membership]
;;          :query      [:can bob-kid :restart "/api"]})
;; ;; => {:allowed? false}
;; ;; bob is not named as ops-team — no path to the capability
;; ```
;;
;; **What to notice:** the reader's job was to supply assertions
;; (one capability, one name-binding) and trust (`:self` trusts
;; root). The library did the name resolution. This is the SDSI
;; pattern: names are assertions, not infrastructure.

;; ---
;; ## Chapter 5 — Delegation
;;
;; New assertion type: `:delegation`. A trusted sayer grants
;; another key the authority to issue certain assertion types.
;;
;; ```clojure
;; ;; Root delegates capability-issuing to team-lead
;; (def del
;;   (assertion {:sayer    :root
;;               :type     :delegation
;;               :delegate team-lead-kid
;;               :scope    :capability}))
;;
;; ;; Team-lead issues a capability (can do so because root
;; ;; delegated that authority)
;; (def cap
;;   (assertion {:sayer    team-lead-kid
;;               :type     :capability
;;               :subject  "alice"
;;               :action   :deploy
;;               :resource "/staging"}))
;;
;; ;; :self only trusts root directly:
;; (decide {:trust      [:root]
;;          :assertions [del cap]
;;          :query      [:can "alice" :deploy "/staging"]})
;; ;; => {:allowed? true}
;; ```
;;
;; Trust flows: `:self` trusts root → root delegated to
;; team-lead → team-lead's capability assertion is now
;; trusted transitively. The reader didn't write a
;; trust-expansion rule. The library's inference rules
;; resolved the delegation chain automatically.
;;
;; **Scoped delegation:**
;;
;; ```clojure
;; ;; Root delegates name-binding (only) to HR
;; (def hr-del
;;   (assertion {:sayer    :root
;;               :type     :delegation
;;               :delegate :hr
;;               :scope    :name-binding}))
;;
;; ;; HR tries to issue a capability — NOPE
;; (def hr-cap
;;   (assertion {:sayer    :hr
;;               :type     :capability
;;               :subject  "alice"
;;               :action   :admin
;;               :resource :everything}))
;;
;; (decide {:trust      [:root]
;;          :assertions [hr-del hr-cap]
;;          :query      [:can "alice" :admin :everything]})
;; ;; => {:allowed? false}
;; ;; HR was delegated for :name-binding only, not :capability
;; ```

;; ---
;; ## Chapter 6 — Revocation
;;
;; New assertion type: `:revocation`. A sayer revokes a
;; previously issued assertion. Revocation has three tiers,
;; each for a progressively more severe situation.
;;
;; ### Tier 1: Revoking a specific assertion
;;
;; The normal case. Root issued a capability and now wants to
;; take it back:
;;
;; ```clojure
;; (def cap
;;   (assertion {:sayer    :root
;;               :type     :capability
;;               :id       "cap-42"
;;               :subject  "alice"
;;               :action   :deploy
;;               :resource "/staging"}))
;;
;; ;; Later: root revokes that specific capability
;; (def revoke
;;   (assertion {:sayer    :root
;;               :type     :revocation
;;               :target   "cap-42"}))
;;
;; (decide {:trust      [:root]
;;          :assertions [cap revoke]
;;          :query      [:can "alice" :deploy "/staging"]})
;; ;; => {:allowed? false}
;; ;; the capability exists but has been revoked
;; ```
;;
;; ### Tier 2: Root key compromise — the revocation authority
;;
;; What if root's private key is stolen? Root can't revoke
;; itself — the attacker has the key and won't cooperate.
;; Reconfiguring `:self` across every service takes time.
;; You need an **emergency brake**.
;;
;; A **revocation authority** is a separate key, configured
;; alongside the regular trust roots, but scoped to
;; revocations only. It's kept offline or in an HSM — it
;; exists solely for this scenario.
;;
;; ```clojure
;; ;; :self configures both at startup:
;; (decide {:trust      [:root
;;                       {:sayer :revocation-authority
;;                        :scope :revocation}]
;;          :assertions [...]
;;          :query      [...]})
;;
;; ;; Root is compromised! The revocation authority signs:
;; (sign-assertion revocation-kp
;;   {:type       :revocation
;;    :target-key root-kid})
;;
;; ;; This assertion propagates through the same channels as
;; ;; any other assertion — every service that receives it
;; ;; immediately stops trusting root. No redeployment.
;; ```
;;
;; The revocation authority is just another trust root that
;; `:self` configured — there's no magic. Its key is also
;; configured, also anchored in `:self`. The difference is
;; operational: it has the narrowest possible scope (can only
;; revoke, can't issue capabilities or names) and the highest
;; security posture (offline, HSM, break-glass access).
;;
;; After revoking the compromised root, you configure a
;; replacement key at your own pace — the signed revocation
;; assertion bought you time.
;;
;; ### Tier 3: Revocation authority compromise
;;
;; If the revocation authority itself is compromised, you're
;; in the nuclear scenario. The only option is reconfiguring
;; `:self` — updating every service's trust root config and
;; redeploying. That's slow, but it's also vanishingly rare:
;; the revocation authority key is offline and almost never
;; used.
;;
;; **The three tiers summarized:**
;;
;; ```
;; Tier 1 — assertion revoked     → root signs a :revocation
;; Tier 2 — root key compromised  → revocation authority signs
;; Tier 3 — revocation authority   → reconfigure :self
;;          compromised              (redeployment)
;; ```
;;
;; Each tier is rarer, more severe, and involves a more
;; secure key. Trust roots are configured — that's
;; irreducible. `:self` has to start somewhere. The
;; revocation authority doesn't eliminate configuration;
;; it just means you don't have to reconfigure everything
;; simultaneously while an attacker is active.
;;
;; **What to notice:** revocation is an assertion, same as
;; everything else. The revocation authority is a configured
;; trust root with a narrow scope. All three tiers use the
;; same primitives — assertions from sayers, trust from
;; `:self`. No special machinery.

;; ---
;; ## Chapter 7 — Signed assertions (signet enters)
;;
;; Until now, sayers were keywords (`:root`, `:hr`). In a real
;; system, sayers are **cryptographic keys** — Ed25519 keypairs
;; that sign their assertions. This is where the signet library
;; enters.
;;
;; ```clojure
;; (require '[signet.key :as key])
;;
;; (def root-kp (key/signing-keypair))
;; (def root-kid (key/kid root-kp))
;; ;; => "urn:signet:pk:ed25519:abc123..."
;;
;; ;; Now :sayer is a real cryptographic identity:
;; (def cap
;;   (sign-assertion root-kp
;;     {:type     :capability
;;      :subject  "alice"
;;      :action   :edit
;;      :resource :posts}))
;;
;; ;; :self trusts this specific key:
;; (decide {:trust      [root-kid]
;;          :assertions [cap]
;;          :query      [:can "alice" :edit :posts]})
;; ;; => {:allowed? true}
;; ```
;;
;; The shape is identical to Chapter 1. The only changes:
;; `:root` became a real key identity (kid URN), and
;; `assertion` became `sign-assertion` (which also derives
;; the `:sayer` from the signing key). Everything else —
;; trust, query, decision — works the same way.
;;
;; The trust roots in a real deployment are kid URNs
;; configured into the service — hard-coded or loaded from
;; config. Either way, they're `:self`'s assertions about
;; which cryptographic identities it trusts.

;; ---
;; ## Chapter 8 — Putting it together
;;
;; A small end-to-end example. Maybe a Ring handler where a
;; client presents signed assertions and the service decides.
;;
;; The reader has learned:
;;
;; - **Assertions** — the only user-facing primitive
;;   (capability, name-binding, delegation, revocation)
;; - **Trust** — assertions by `:self`, the runtime's own
;;   trust anchor; configured or hard-coded, grounding all
;;   trust in the runtime itself
;; - **Inference rules** — library-shipped, never written by
;;   the user, define how assertion types compose
;; - **`decide`** — assertions in, question in, answer out
;; - **Signed assertions** — same model, real crypto
;;
;; The whole mental model fits on an index card:
;;
;; ```
;; ┌─────────────────────────────────────────────┐
;; │  The one axiom:                              │
;; │    [:trusted :self]                          │
;; │                                              │
;; │  You provide:                                │
;; │    trust   (who :self trusts — from config   │
;; │             or hard-coded in the service)    │
;; │    assertions  (from sayers :self may trust) │
;; │    query       (what you want to know)       │
;; │                                              │
;; │  The library provides:                       │
;; │    inference rules  (how assertions compose) │
;; │    engine           (exhaustive evaluation)  │
;; │                                              │
;; │  You get back:                               │
;; │    {:allowed? true/false}                    │
;; └─────────────────────────────────────────────┘
;; ```

;; ---
;; ## Appendix — The inference rules (for the curious)
;;
;; The library ships these. The reader never writes them.
;; Showing them here for transparency.
;;
;; ```clojure
;; ;; 0. The axiom — :self is trusted (library-provided fact)
;; [:trusted :self]
;;
;; ;; 1. Trust expansion — :self's trust assertions activate sayers
;; {:head [:trusted ?subject]
;;  :body [[:asserts ?truster :trust ?subject]
;;         [:trusted ?truster]]}
;;
;; ;; 2. Capability unpacking — trusted capability → [:can ...]
;; {:head [:can ?subject ?action ?resource]
;;  :body [[?sayer ?ts ?nb ?na :capability ?id
;;          ?subject ?action ?resource]
;;         [:trusted ?sayer]]}
;;
;; ;; 3. Name-binding unpacking — trusted name-binding → [:named ...]
;; {:head [:named ?key ?name]
;;  :body [[?sayer ?ts ?nb ?na :name-binding ?id
;;          ?key ?name]
;;         [:trusted ?sayer]]}
;;
;; ;; 4. Name resolution — named key inherits group capabilities
;; {:head [:can ?key ?action ?resource]
;;  :body [[:named ?key ?group]
;;         [:can ?group ?action ?resource]]}
;;
;; ;; 5. Delegation trust expansion — trusted delegation → trust delegate
;; {:head [:trusted ?delegate]
;;  :body [[?sayer ?ts ?nb ?na :delegation ?id
;;          ?delegate ?scope]
;;         [:trusted ?sayer]]}
;;
;; ;; 6. Revocation filtering (pre-evaluation pass)
;; ;; ... handled before Datalog runs
;; ```
;;
;; Six rules plus one axiom. That's the entire authorization
;; logic for the SPKI/SDSI model. Everything else is assertions.
;;
;; Notice that rule 1 (trust expansion) and rule 5 (delegation
;; trust expansion) both produce `[:trusted ?someone]`. They're
;; the same pattern: `:self` trusts root because `:self` asserted
;; it; root trusts team-lead because root delegated to them. The
;; Datalog fixpoint chains these together automatically — trust
;; radiates outward from `:self` through whatever assertions and
;; delegations exist.
