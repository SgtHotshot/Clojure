(ns leiningen.light-nrepl
  (:gen-class)
  (:require [leiningen.core.project :as lp]
            [leiningen.repl :as repl]
            [clojure.string :as string]
            [fs.core :as fs])
  (:use [leiningen.core.eval :only [eval-in-project]]))

(defn proj->name [proj]
  (str (:name proj) " " (:version proj)))

(defn prep [project name]
  (let [init `(swap! lighttable.nrepl.core/my-settings merge {:name ~(or name (str (:name project) " " (:version project))) :project (quote ~project)})
        init (if-let [cur-init (-> project :repl-options :init)]
               (list 'do cur-init init)
               init)
        profile {:dependencies '[[lein-light-nrepl/lein-light-nrepl "0.0.9"]
                                 [org.clojure/tools.reader "0.7.10"]]
                 :repl-options {:nrepl-middleware ['lighttable.nrepl.handler/lighttable-ops]
                                 :init (with-meta init {:replace true})}}
        project (lp/merge-profiles project [profile])]
    (println "final project: " (pr-str (:dependencies project)))
      project))

(defn find-clojure-version [proj]
  (let [deps (:dependencies proj)]
    (second (first (filter #(= (first %) 'org.clojure/clojure) deps)))))

(defn light
  "Start a Light Table client for this project"
  [project & [name]]
  (when (< 0 (compare "1.5.1" (find-clojure-version project)))
    (binding [*out* *err*]
      (println "Light Table requires Clojure Version 1.5.1 or higher")
      (System/exit 0)))
  (try
    (repl/repl (prep project name) ":headless")
    (catch Exception e
      (.printStackTrace e)
      (System/exit 1))))

(defn check-project [path]
  (when (fs/exists? path)
    path))

(defn -main [& [name]]
  (let [path (str (fs/absolute-path fs/*cwd*) "/project.clj")]
    (if (check-project path)
      (light (lp/init-project (lp/read path)) name)
      (binding [*out* *err*]
        (println "Could not find project.clj file at: " path)))))