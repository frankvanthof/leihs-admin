(ns leihs.admin.front.breadcrumbs
  (:require
    [leihs.admin.paths :as paths :refer [path]]
    [leihs.admin.front.state :as state]))

(defn li
  ([k n]
   (li k n {} {}))
  ([handler-key inner route-params query-params]
   (let [active? (= (-> @state/routing-state* :handler-key) handler-key)]
     [:li.breadcrumb-item {:key handler-key :class (if active? "active" "")}
      (if active?
        [:span inner]
        [:a {:href (path handler-key route-params query-params)} inner])])))

(defn api-token-li [user-id api-token-id]
  (li :api-token [:span " API-Token "] {:user-id user-id :api-token-id api-token-id} {}))
(defn api-tokens-li [id]
  (li :api-tokens [:span " API-Tokens "] {:user-id id} {}))
(defn api-token-new-li [id]
  (when (= id (:id @state/user*))
    (li :api-token-new [:span [:i.fas.fa-plus-circle] " New API-Token "] {:user-id id} {})))
(defn api-token-edit-li [user-id api-token-id]
  (when (= user-id (:id @state/user*))
    (li :api-token-edit [:span [:i.fas.fa-edit] " Edit API-Token "]
        {:user-id user-id :api-token-id api-token-id} {})))
(defn api-token-delete-li [user-id api-token-id]
  (when (= user-id (:id @state/user*))
    (li :api-token-delete [:span [:i.fas.fa-times] " Delete API-Token "]
        {:user-id user-id :api-token-id api-token-id} {})))
(defn admin-li [] (li :admin "Admin"))
(defn auth-li [] (li :auth "Authentication"))
(defn auth-password-sign-in-li [] (li :auth-password-sign-in "Password sign-in"))
(defn borrow-li [] (li :borrow "Borrow"))
(defn debug-li [] (li :debug "Debug"))
(defn email-li [address] [:li.breadcrumb-item {:key (str "mailto:" address )} [:a {:href (str "mailto:" address )} [:i.fas.fa-envelope] " Email "]])
(defn initial-admin-li [] (li :initial-admin "Initial-Admin"))
(defn leihs-li [] (li :leihs "Home"))
(defn lend-li [] (li :lend "Lend"))
(defn procure-li [] (li :procure "Procure"))
(defn request-li [id] (li :request "Request" {:id id} {}))
(defn requests-li [] (li :requests "Requests"))
(defn user-delete-li [id] (li :user-delete [:span [:i.fas.fa-times] " Delete "] {:user-id id} {}))
(defn user-edit-li [id] (li :user-edit [:span [:i.fas.fa-edit] " Edit "] {:user-id id} {}))
(defn user-li [id] (li :user "User" {:user-id id} {}))
(defn user-new-li [] (li :user-new [:span [:i.fas.fa-plus-circle] " New user "]))
(defn users-li [] (li :users "Users" {} (:users-query-params @state/global-state*)))

(defn nav-component [left right]
  [:div.row
   [:nav.col-lg {:aria-label :breadcrumb :role :navigation}
    (when (seq left)
      [:ol.breadcrumb
       (for [li left] li) ])]
   [:nav.col-lg {:role :navigation}
    (when (seq right)
      [:ol.breadcrumb.leihs-nav-right
       (for [li right] li)])]])
