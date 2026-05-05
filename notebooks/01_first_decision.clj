;; # Chapter 1 — Your first authorization decision
;;
;; Suppose you're building a small blog. Someone hits
;; `POST /posts/42/edit` and your service has to decide: **do we let
;; them through?**
;;
;; That's an authorization question, and it's exactly the kind of
;; thing **stroopwafel** exists to answer. Stroopwafel is a tiny
;; Clojure library that takes some facts you know, a rule or two,
;; and a question — and gives you back a yes or a no. That's the
;; whole library. One function does the work: `evaluate`.
;;
;; Let's see it run.

(ns first-decision
  (:require [stroopwafel.core :as sw]))

;; ## Facts
;;
;; Facts are Clojure vectors. Stroopwafel matches them by *position*
;; — it doesn't know or care what any slot "means." So a fact like
;; `[:has-role "alice" :editor]` would work just fine: the engine
;; would read slot 1 as the user and slot 2 as the role because
;; that's where we chose to put them.
;;
;; The trouble is that positional tuples get hard to read the
;; moment there are more than two or three slots, and we're going
;; to grow them quickly. So in this tutorial we'll adopt a
;; convention from the very first fact: **put a label keyword in
;; front of every value**, and use commas to break the vector up
;; visually.

(def blog-facts
  [[:has-role, :user "alice", :role :editor]
   [:has-role, :user "bob",   :role :viewer]])

;; Two things to notice about that shape. First, the commas are
;; whitespace in Clojure — they're purely visual, and they let the
;; vector read like a little map. Second, the labels (`:user`,
;; `:role`) are literal keywords the engine ignores: they unify
;; against themselves, which always succeeds, so they cost us
;; nothing at match time. They're **readability scaffolding**, not
;; semantics. Drop them and the engine would still do the same
;; thing.
;;
;; Two facts, then: alice is an editor, bob is a viewer.

;; ## A rule
;;
;; Facts on their own only tell us what we typed in. A **rule**
;; lets us *derive* new facts from existing ones. Here's one that
;; says "editors can edit posts":

(def blog-rules
  '[{:head [:can-edit, :user ?user]
     :body [[:has-role, :user ?user, :role :editor]]}])

;; Read it like English: *"there's a fact `[:can-edit :user ?user]`
;; whenever there's a fact `[:has-role :user ?user :role :editor]`."*
;; The `?user` is a variable — stroopwafel fills it in from whatever
;; facts match the body. The labels in the rule body line up with
;; the labels in the fact, so you can see at a glance which piece
;; of the fact the `?user` variable is capturing.

;; ## A decision
;;
;; Now we ask the engine a question: **can alice edit?**

(sw/evaluate
 {:blocks [{:facts blog-facts
            :rules blog-rules}]}
 :authorizer
 {:policies [{:kind  :allow
              :query [[:can-edit, :user "alice"]]}]})

;; The rule fires: alice has role `:editor`, so the derived fact
;; `[:can-edit :user "alice"]` exists. The policy's query matches
;; that fact. Decision: **allowed**.
;;
;; Change the name and the decision flips:

(sw/evaluate
 {:blocks [{:facts blog-facts
            :rules blog-rules}]}
 :authorizer
 {:policies [{:kind  :allow
              :query [[:can-edit, :user "bob"]]}]})

;; Bob's role is `:viewer`, not `:editor`, so the rule body doesn't
;; match for him, no `[:can-edit :user "bob"]` fact is ever
;; derived, no policy fires, and the default is **deny**.

;; ## What to notice
;;
;; The engine doesn't know what `:has-role`, `:can-edit`, or
;; `editor` mean. It just matches patterns against vectors. That
;; ignorance is the whole point: the same machine works for file
;; permissions, workflow gating, feature flags, agent authorization
;; — anything you can phrase as *facts, rules, and a question*.
;;
;; You just ran a real authorization decision in about a dozen
;; lines of Clojure. The next chapter pauses on an uncomfortable
;; question — *who told us alice is an editor, and why did we
;; believe them?* — before we resume typing code.

^{:kind/hiccup true :kindly/hide-code true}
[:div {:style "margin-top:2em;padding-top:1em;border-top:1px solid #ddd;text-align:right;"}
 [:a {:href "who_said_so.html"
      :style (str "display:inline-block;"
                  "padding:0.6em 1.2em;"
                  "background:#0366d6;"
                  "color:white;"
                  "text-decoration:none;"
                  "border-radius:4px;"
                  "font-family:sans-serif;"
                  "font-weight:500;")}
  "Next: Chapter 2 — But who said so? →"]]
