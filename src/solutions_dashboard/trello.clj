(ns solutions-dashboard.trello 
  (:require
   [postal.core :as postal]
   [solutions-dashboard.config :as config]
   [clojure.data.json  :as json]
   [clj-http.client    :as client]))


(def trello-url "https://api.trello.com/")
(def version 1) ;; version of the trello api


(defn make-trello-api-call [method uri query-params]
  (json/read-json
   (:body (client/request
           {:method method
            :content-type :json
            :accept :json
            :query-params (merge query-params
                                 {:key config/trello-key
                                  :token config/trello-token})
            :url (str trello-url version "/" uri)}))))

(defn get-organization [id]
  (make-trello-api-call :get (str "organizations/"  id) {}))

(defn get-opengeo []
  (get-organization config/trello-opengeo-id))

(defn get-opengeo-people []
  (:members (make-trello-api-call :get (str "organization/" config/trello-opengeo-id ) {:members "all"})))

(defn get-tasks-by-user [name]
  (:cards (make-trello-api-call :get (str "members/" name)  {:cards "all" :card_fields "all"})))

(defn get-boards-by-user [name]
  (:boards (make-trello-api-call :get (str "members/" name) {:boards "all" :board_fields "all"})))

(defn get-organization-boards [org]
  (make-trello-api-call :get (str "organizations/" org "/boards") {}))


(defn get-user-projects
  "Function to query the trello api. Finds all of the boards and cards
  associated with a user. Groups the cards by what boards they are
  associated with.
  TODO FIX ME.

  In the solutions dashboard boards are projects and
  cards are task."
  [person]
  (let [user-info (make-trello-api-call :get
                                       (str "members/" person)
                                       {:boards "all" :board_fields "all" :cards "all" :card_fields "all"})
        grouped-tasks (group-by :idBoard (:cards user-info))]
    (dissoc  (assoc user-info :projects
                    (for [project (:boards user-info)]
                      (assoc project  :tasks (get grouped-tasks (:id project)))))  :boards :cards)))


(defn send-test-email [user password]
  (postal/send-message
   #^{:host "smtp.gmail.com"
      :user user
      :pass password
      :ssl :yes!!!11}
   {:from "iwillig@gmail.com"
    :to ["iwillig@opengeo.org"]
    :subject "hello"
    :body "this is a test" }))

(defn -main [& args]
  (let [people (get-opengeo-people)]
    (doseq [person people]
      (time (get-user-projects (:username person))))))