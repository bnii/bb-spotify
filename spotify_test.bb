#!/usr/bin/env bb

(require '[clojure.test :refer [deftest is testing run-tests]]
         '[clojure.string :as str])

(load-file "spotify")

;; ── Test Data ────────────────────────────────────────────────────────────────

(def test-devices
  [{:id "abc123" :name "MacBook Pro" :type "Computer" :is_active true}
   {:id "def456" :name "Echo Dot"    :type "Speaker"  :is_active false}
   {:id "ghi789" :name "iPhone"      :type "Smartphone" :is_active false}])

(def test-playlists
  [{:id "pl1" :name "Jazz Blues Essentials" :uri "spotify:playlist:pl1"}
   {:id "pl2" :name "Blue Train"            :uri "spotify:playlist:pl2"}
   {:id "pl3" :name "My Favorites"          :uri "spotify:playlist:pl3"}])

;; ── match-device ─────────────────────────────────────────────────────────────

(deftest test-match-device
  (testing "exact ID match"
    (is (= "def456" (:id (bb-spotify/match-device test-devices "def456")))))

  (testing "case-insensitive name substring"
    (is (= "abc123" (:id (bb-spotify/match-device test-devices "macbook"))))
    (is (= "def456" (:id (bb-spotify/match-device test-devices "echo"))))
    (is (= "def456" (:id (bb-spotify/match-device test-devices "DOT")))))

  (testing "prefers exact ID over name substring"
    ;; device whose name contains "abc123" shouldn't interfere with ID lookup
    (let [devs (conj test-devices {:id "zzz" :name "device-abc123" :type "X"})]
      (is (= "abc123" (:id (bb-spotify/match-device devs "abc123"))))))

  (testing "returns nil on no match"
    (is (nil? (bb-spotify/match-device test-devices "nonexistent")))))

;; ── match-playlist ───────────────────────────────────────────────────────────

(deftest test-match-playlist
  (testing "exact name match (case-insensitive)"
    (is (= "pl1" (:id (bb-spotify/match-playlist test-playlists "Jazz Blues Essentials"))))
    (is (= "pl1" (:id (bb-spotify/match-playlist test-playlists "jazz blues essentials")))))

  (testing "substring match"
    (is (= "pl2" (:id (bb-spotify/match-playlist test-playlists "Blue Tr"))))
    (is (= "pl3" (:id (bb-spotify/match-playlist test-playlists "favor")))))

  (testing "prefers exact over substring"
    (let [lists [{:id "a" :name "Blue" :uri "spotify:playlist:a"}
                 {:id "b" :name "Blue Train" :uri "spotify:playlist:b"}]]
      (is (= "a" (:id (bb-spotify/match-playlist lists "Blue"))))))

  (testing "returns nil on no match"
    (is (nil? (bb-spotify/match-playlist test-playlists "nonexistent")))))

;; ── tokens-expired? ──────────────────────────────────────────────────────────

(deftest test-tokens-expired?
  (testing "expired token"
    (is (true? (bb-spotify/tokens-expired? {:expires-at 0}))))

  (testing "valid token (far future)"
    (is (false? (bb-spotify/tokens-expired?
                  {:expires-at (+ (System/currentTimeMillis) 3600000)}))))

  (testing "within 60s buffer is treated as expired"
    (is (true? (bb-spotify/tokens-expired?
                 {:expires-at (+ (System/currentTimeMillis) 30000)})))))

;; ── complete! (static branches) ──────────────────────────────────────────────

(defn completions
  "Simulate bash completion. Pass all COMP_WORDS including the partial word being
   completed (use \"\" for empty). cword is the last index."
  [& words]
  (let [cword (str (dec (count words)))
        args  (into [cword] words)
        out   (str/trim (with-out-str (bb-spotify/complete! args)))]
    (if (str/blank? out) [] (str/split-lines out))))

(deftest test-complete-commands
  (testing "lists all commands at position 1"
    (let [cmds (completions "spotify" "")]
      (is (every? (set cmds) ["auth" "search" "playlists" "playlist" "devices"
                               "device" "play" "pause" "resume" "next" "prev"
                               "queue" "now"]))))

  (testing "filters by prefix"
    (is (= ["playlists" "playlist" "play" "pause" "prev"]
           (completions "spotify" "p")))
    (is (= ["playlists" "playlist" "play"]
           (completions "spotify" "pl")))
    (is (= ["next" "now"] (completions "spotify" "n")))
    (is (= ["search"] (completions "spotify" "s")))))

(deftest test-complete-playlist-subcommands
  (testing "playlist subcommands"
    (let [subs (completions "spotify" "playlist" "")]
      (is (= #{"create" "show" "add" "remove"} (set subs)))))

  (testing "playlist subcommand prefix filter"
    (is (= ["show"] (completions "spotify" "playlist" "sh")))))

(deftest test-complete-device-play
  (testing "suggests play after device <name>"
    (is (= ["play"] (completions "spotify" "device" "mydev" "")))))

(deftest test-complete-dynamic-devices
  (testing "completes with device names and IDs"
    (with-redefs [bb-spotify/api! (fn [_ _] {:devices test-devices})]
      (let [candidates (completions "spotify" "device" "")]
        (is (some #{"MacBook Pro"} candidates))
        (is (some #{"Echo Dot"} candidates))
        (is (some #{"abc123"} candidates))))))

(deftest test-complete-dynamic-playlists
  (testing "completes play with playlist names"
    (with-redefs [bb-spotify/paginate (fn [_ _] test-playlists)]
      (let [candidates (completions "spotify" "play" "")]
        (is (some #{"Jazz Blues Essentials"} candidates))
        (is (some #{"Blue Train"} candidates))))))

;; ── cmd-play routing ─────────────────────────────────────────────────────────

(deftest test-cmd-play-routing
  (let [calls (atom [])]
    (with-redefs [bb-spotify/api! (fn [method path & [opts]]
                                    (swap! calls conj {:method method :path path :opts opts})
                                    nil)]
      (testing "no args resumes playback"
        (reset! calls [])
        (with-out-str (bb-spotify/cmd-play []))
        (is (= :put (:method (first @calls))))
        (is (str/includes? (:path (first @calls)) "/me/player/play")))

      (testing "track URI sends :uris"
        (reset! calls [])
        (with-out-str (bb-spotify/cmd-play ["spotify:track:abc"]))
        (let [body (get-in (first @calls) [:opts :body])]
          (is (str/includes? body "\"uris\""))))

      (testing "playlist URI sends :context_uri"
        (reset! calls [])
        (with-out-str (bb-spotify/cmd-play ["spotify:playlist:xyz"]))
        (let [body (get-in (first @calls) [:opts :body])]
          (is (str/includes? body "\"context_uri\"")))))))

(deftest test-cmd-play-by-name
  (let [calls (atom [])]
    (with-redefs [bb-spotify/api!      (fn [method path & [opts]]
                                         (swap! calls conj {:method method :path path :opts opts})
                                         nil)
                  bb-spotify/paginate   (fn [_ _] test-playlists)]
      (testing "bare name resolves playlist and plays context_uri"
        (reset! calls [])
        (with-out-str (bb-spotify/cmd-play ["Jazz Blues"]))
        ;; first call is paginate (mocked), second is the play API call
        (let [play-call (last @calls)
              body (:body (:opts play-call))]
          (is (str/includes? body "spotify:playlist:pl1")))))))

;; ── cmd-queue branching ──────────────────────────────────────────────────────

(deftest test-cmd-queue-show
  (let [calls (atom [])]
    (with-redefs [bb-spotify/api! (fn [method path & [opts]]
                                    (swap! calls conj {:method method :path path})
                                    {:currently_playing {:name "Test Track"
                                                        :artists [{:name "Artist"}]}
                                     :queue []})]
      (testing "no args shows queue (GET)"
        (with-out-str (bb-spotify/cmd-queue []))
        (is (= :get (:method (first @calls))))
        (is (str/includes? (:path (first @calls)) "/me/player/queue"))))))

(deftest test-cmd-queue-add
  (let [calls (atom [])]
    (with-redefs [bb-spotify/api! (fn [method path & [opts]]
                                    (swap! calls conj {:method method :path path})
                                    nil)]
      (testing "with URI adds to queue (POST)"
        (with-out-str (bb-spotify/cmd-queue ["spotify:track:abc"]))
        (is (= :post (:method (first @calls))))
        (is (str/includes? (:path (first @calls)) "/me/player/queue"))))))

;; ── Run ──────────────────────────────────────────────────────────────────────

(let [{:keys [fail error]} (run-tests)]
  (System/exit (if (zero? (+ fail error)) 0 1)))
