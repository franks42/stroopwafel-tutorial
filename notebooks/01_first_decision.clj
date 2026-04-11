;; # Chapter 1 — Your first authorization decision
;;
;; > **Note to the tutorial author (fresh Claude instance or human):**
;; > This file is a SCAFFOLD, not a finished chapter. It demonstrates
;; > the Clay notebook format and confirms the three libraries are
;; > wired up correctly. The actual prose — the hook, the explanation,
;; > the "aha" moment — you write. See ../HANDOFF.md §5 and §9 for
;; > the pedagogical arc. Delete this note before shipping.

;; This tutorial is about a Clojure library called **stroopwafel**.
;; Despite the name, it's not a food — it's a tiny machine for making
;; yes/no decisions.
;;
;; Here's the whole idea in one sentence: **you give stroopwafel some
;; facts, ask it a question, and it gives you a yes/no answer.**
;;
;; That's it. Let's see it work.

(ns first-decision
  (:require [stroopwafel.core :as sw]))

;; ## The three ingredients
;;
;; Every decision stroopwafel makes comes from three things:
;;
;; 1. **Facts** — what we know to be true
;; 2. **Query** — what we want to check
;; 3. **Policy** — a rule that says "if the query succeeds, allow"
;;
;; Let's build the smallest possible example.

;; ## Facts
;;
;; Facts are Clojure vectors. Each vector is one fact. The first
;; element is a conventional "predicate" (like a column name), and
;; the rest are the values. Stroopwafel doesn't care what any of
;; this *means* — it just stores the vectors and matches patterns
;; against them.

(def my-facts
  [[:user "alice"]
   [:role "alice" "admin"]])

;; Two facts: "there is a user named alice" and "alice has the role
;; admin". Nothing magical — they're just data.

;; ## A decision
;;
;; Now we ask stroopwafel: "is alice an admin?"

(sw/evaluate
 ;; The token — our collection of fact blocks
 {:blocks [{:facts my-facts}]}
 ;; The authorizer — our policies
 :authorizer
 {:policies [{:kind  :allow
              :query [[:role "alice" "admin"]]}]})

;; → `{:valid? true}`
;;
;; The policy says "allow if `[:role "alice" "admin"]` is in the
;; facts." It is. Decision: allowed.
;;
;; Change the policy to ask a different question and the decision
;; flips:

(sw/evaluate
 {:blocks [{:facts my-facts}]}
 :authorizer
 {:policies [{:kind  :allow
              :query [[:role "alice" "cto"]]}]})

;; → `{:valid? false}`
;;
;; No fact matches `[:role "alice" "cto"]`, so the policy doesn't
;; trigger, and the default is deny.

;; ## What's next
;;
;; Chapter 1 showed one fact, one policy, one decision. Real
;; authorization needs more — **variables**, **rules that derive new
;; facts from existing ones**, **checks that must pass**, and
;; eventually **signed facts from identified parties**. All of that
;; is the next few chapters.
;;
;; The thing to notice here: stroopwafel doesn't know what `:role`
;; means. It just pattern-matches. That ignorance is the whole
;; point — it means the same engine works for authentication,
;; authorization, workflow gating, access control, policy checks,
;; and anything else you can shape like facts-and-queries.
