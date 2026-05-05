;; # Chapter 3 — Making it real: assertions and trust
;;
;; In Chapter 2 we promised that from here on every fact would
;; carry an explicit **sayer**, and that **trust itself would be a
;; fact** we could reason about. This chapter makes it real. Same
;; tiny library, same `evaluate`, same pattern-matching engine. No
;; crypto yet. Just data — arranged with a little more discipline.

(ns assertions-and-trust
  (:require [stroopwafel.core :as sw]))

;; ## The new fact shape
;;
;; Let's start with a single assertion: *root says alice can edit
;; posts*. In the labeled-vector form we adopted in Chapter 1, it
;; looks like this:

(def one-assertion
  [:asserts, :sayer :root, :subject "alice", :action :edit, :resource :posts])

;; Four slots now instead of Chapter 1's two. The labels carry
;; their weight — you can see at a glance what each value means —
;; but typing this pattern out repeatedly is going to get tedious
;; fast. Hold that thought.

;; ## The rule that joins assertions with trust
;;
;; We need a rule that says: *"we derive `[:can ...]` about a
;; subject/action/resource whenever some sayer **we trust** has
;; asserted it."* Here it is:

(def rules
  '[{:head [:can,     :subject ?s,    :action ?a, :resource ?r]
     :body [[:asserts, :sayer ?sayer, :subject ?s,
                       :action ?a,    :resource ?r]
            [:trust,   :sayer ?sayer]]}])

;; The key move: the variable `?sayer` appears in **both** body
;; clauses. Datalog only counts the rule as firing when the same
;; name is both in an assertion and in a trust fact. That's what
;; "trusted sayer" means, mechanically. There's nothing else to
;; it.

;; ## A decision, the slow way
;;
;; Before we introduce any convenience, let's run a real decision
;; with everything typed out by hand. Squint a little — this is
;; the part we're about to fix.

(sw/evaluate
 {:blocks
  [{:facts [[:asserts, :sayer :root, :subject "alice", :action :edit, :resource :posts]
            [:asserts, :sayer :root, :subject "bob",   :action :read, :resource :posts]
            [:trust,   :sayer :root]]
    :rules rules}]}
 :authorizer
 {:policies [{:kind  :allow
              :query [[:can, :subject "alice", :action :edit, :resource :posts]]}]})

;; That works — you'll see `{:valid? true}` in the result block —
;; but counting `:sayer` and `:subject` labels in the source is
;; already unpleasant at two facts. In a real system we'd have
;; hundreds. Let's write a tiny construction helper.

;; ## The `labeled` helper
;;
;; Four lines of user-space Clojure. Given a **tag**, a
;; **slot-order vector** (the schema — which keyword goes in which
;; position), and a **map of values**, it produces the labeled
;; vector we've been writing by hand. It can also take a
;; **defaults** map, so common fields don't need to be repeated
;; at every call site.

(defn labeled
  "Build a labeled-vector fact from a tag, a slot-order vector,
   a values map, and optional defaults."
  ([tag slots m] (labeled tag slots m {}))
  ([tag slots m defaults]
   (let [m (merge defaults m)]
     (into [tag] (mapcat (fn [k] [k (get m k)]) slots)))))

;; On top of `labeled` we build two *flavored* constructors — one
;; for assertions, one for trust — that bake in the slot order and
;; sensible defaults:

(defn asserts [m]
  (labeled :asserts
           [:sayer :subject :action :resource]
           m
           {:sayer :root}))

(defn trust [m]
  (labeled :trust [:sayer] m))

;; Let's see what `asserts` produces when we call it:

(asserts {:subject "alice", :action :edit, :resource :posts})

;; The helper returned **the same labeled vector we would have
;; typed by hand** — same shape, same slot order, same labels.
;; Nothing is hidden. The engine has no idea a helper was
;; involved; it just sees a vector.
;;
;; Notice `:sayer :root` appeared in the output even though we
;; didn't pass `:sayer` — that's the default doing its job. When
;; we want a different sayer we just override:

(asserts {:sayer :hr, :subject "alice", :action :edit, :resource :posts})

;; Override wins, as you'd expect from `merge`.

;; ## The decision, the readable way
;;
;; Here's the same decision from earlier, rewritten with the
;; constructors:

(sw/evaluate
 {:blocks
  [{:facts [(asserts {:subject "alice", :action :edit, :resource :posts})
            (asserts {:subject "bob",   :action :read, :resource :posts})
            (trust   {:sayer :root})]
    :rules rules}]}
 :authorizer
 {:policies [{:kind  :allow
              :query [[:can, :subject "alice", :action :edit, :resource :posts]]}]})

;; Same decision. Same engine. Much less squinting.
;;
;; Notice what did **not** change: the rule body (back up in
;; `rules`) and the policy query are still labeled-vector patterns
;; with `?variables`, because they *need* to be. Datalog unifies
;; by where a variable sits in the tuple, and the `labeled` helper
;; doesn't help us write patterns — it helps us construct facts.
;; **The helper is for building data, not for querying it.** Keep
;; that line in your head; it's the one thing about the helper
;; that's easy to get wrong.

;; ## Trust is just data
;;
;; Now the payoff for putting sayers in the data. Watch what
;; happens when we pull the trust fact out:

(sw/evaluate
 {:blocks
  [{:facts [(asserts {:subject "alice", :action :edit, :resource :posts})
            (asserts {:subject "bob",   :action :read, :resource :posts})
            ;; no trust fact at all — we stopped trusting :root
            ]
    :rules rules}]}
 :authorizer
 {:policies [{:kind  :allow
              :query [[:can, :subject "alice", :action :edit, :resource :posts]]}]})

;; Alice still has a perfectly good assertion saying she can edit
;; posts. Root is still the sayer. Nothing about the assertion
;; itself changed. But without `[:trust :sayer :root]` in the
;; fact store, the rule's second body clause never matches — so
;; the derived `[:can ...]` fact never comes into existence — so
;; the policy finds nothing to match — so the decision is
;; **deny**.
;;
;; That is the whole idea of this tutorial in one flipped bit.
;; Authorization isn't a question about what assertions "really
;; say." It's a question about **what you're willing to act on,
;; given who said it, and whether you currently trust them**.
;; Trust is a fact, stored alongside everything else, and flipping
;; it flips the decision. Not by magic — by pattern matching, the
;; same mechanism Chapter 1 built its whole house on.

;; ## What to notice
;;
;; - **Constructors, not engine changes.** We didn't teach
;;   stroopwafel anything new. `labeled`, `asserts`, and `trust`
;;   are four, three, and two lines of ordinary Clojure,
;;   respectively. The engine still sees plain labeled vectors.
;; - **Defaults are just `merge`.** If you wanted more slot
;;   orders, or different defaults, you'd write more flavored
;;   constructors on top of `labeled`. Nothing special about the
;;   ones we wrote.
;; - **Trust is data.** You revoke by removing a fact. You
;;   delegate by adding one. You change your mind by editing a
;;   vector. Later chapters make this more sophisticated — signed
;;   trust roots, scoped trust, chains of delegation — but
;;   mechanically it stays *just more facts*.
;;
;; Chapter 4 starts breaking the assumption we snuck in here: that
;; every sayer we trust is trusted for *everything*. We'll meet
;; **scoped trust** — trusting HR to say who's an employee but
;; not who's an admin — and, unsurprisingly, we'll find that the
;; mechanism is just another rule over another fact.

^{:kind/hiccup true :kindly/hide-code true}
[:div {:style "margin-top:2em;padding-top:1em;border-top:1px solid #ddd;display:flex;justify-content:space-between;"}
 [:a {:href "who_said_so.html"
      :style (str "display:inline-block;"
                  "padding:0.6em 1.2em;"
                  "background:#f3f4f6;"
                  "color:#0366d6;"
                  "text-decoration:none;"
                  "border:1px solid #d1d5db;"
                  "border-radius:4px;"
                  "font-family:sans-serif;"
                  "font-weight:500;")}
  "← Previous: Chapter 2"]
 [:span {:style "color:#9ca3af;font-family:sans-serif;font-style:italic;padding:0.6em 0;"}
  "Chapter 4 coming soon"]]
