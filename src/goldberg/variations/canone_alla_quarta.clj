;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Functional Composition                                    ;;
;; by Chris Ford - @ctford                                   ;;
;; (ThoughtWorks)                                            ;;
;;                                                           ;;
;; http://github.com/ctford/functional-composition           ;;
;; http://github.com/overtone/overtone                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(ns goldberg.variations.canone-alla-quarta
  (:use [overtone.live :exclude
          [scale pitch midi->hz note sharp flat run]]
        [quil.core :only
          [smooth sketch ellipse frame-rate background
           width height stroke stroke-weight fill]]))









;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Sine waves                                                ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(definst tone [frequency 440] (sin-osc frequency))
(definst doubletone [freq1 440 freq2 440]
  (+
    (sin-osc freq1)
    (sin-osc freq2)))

(definst beep [frequency 440 duration 1]
  (let [envelope (line 1 0 duration :action FREE)]
          (* envelope (sin-osc frequency))))

;(tone 300)
;(doubletone 300 300)
;(beep 300)
;(stop)





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Harmonics                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(definst bell [frequency 440 duration 10
               h0 1 h1 0.6 h2 0.4 h3 0.25 h4 0.2 h5 0.15]
  (let [harmonics   [ 1  2  3  4  5 6 ]
        proportions [h0 h1 h2 h3 h4 h5]
        proportional-partial
         (fn [harmonic proportion]
           (let [envelope
                  (env-gen (perc 0.01 (* proportion duration)))
                 overtone
                  (* harmonic frequency)]
             (* 1/2 proportion envelope (sin-osc overtone))))
        partials
          (map proportional-partial harmonics proportions)
        whole (mix partials)]
    (detect-silence whole :action FREE)
    whole))

;(bell 300)
;(beep 300)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Equal temperament                                         ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn midi->hz [pitch]
    (*
      8.1757989156 ; midi zero
      (java.lang.Math/pow 2 (/ pitch 12))))

;(midi->hz 69)

(defn ding! [midi] (bell (midi->hz midi) 3))











;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Musical events                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn note [timing pitch] {:time timing :pitch pitch}) 

(defn play! [notes] 
  (let [start (now)]
    (doseq [{ms :time midi :pitch} notes]
      (at (+ ms start) (ding! midi)))
    notes))

(defn even-melody! [pitches]
  (let [times (reductions + (repeat 1000/3))
        notes (map note times pitches)]
    (play! notes)))

;(even-melody! (range 70 81))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scale                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro defs [names values]
  `(do ~@(map
           (fn [name value] `(def ~name ~value))
           names (eval values))))

(defn sum-n [series n] (reduce + (take n series)))

(defn scale [intervals]
  (fn [degree]
    (if-not (neg? degree)
      (sum-n (cycle intervals) degree)
      ((comp - (scale (reverse intervals)) -) degree))))

(def major (scale [2 2 1 2 2 2 1]))

(defn from [offset] (partial + offset))
(def C (from 60))

(defs [D E F G A B]
  (map
    (comp from C major)
    (rest (range))))

(defs [sharp flat] [inc dec])

; alternative scales
(def minor (scale [2 1 2 2 1 2 2]))
(def blues (scale [3 2 1 1 3 2]))
(def pentatonic (scale [3 2 2 3 2]))
(def chromatic (scale [1]))

;(even-melody!
;  (map
;    (comp C major)
;    (concat (range 0 8) (reverse (range 0 7))))
;)

;(even-melody!
;  (let [_ -100]
;    (map (comp D major) [0 1 2 0, 0 1 2 0, 2 3 4 _, 2 3 4 _]))
;)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Melody                                                    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def row-row-row-your-boat
  (let [pitches
         [0 0 0 1 2
          ; Row, row, row your boat,
          2 1 2 3 4 
          ; Gently down the stream,
          7 7 7 4 4 4 2 2 2 0 0 0 
          ; (take 4 (repeat "merrily"))
          4 3 2 1 0]
          ; Life is but a dream!
        durations
         [1 1 2/3 1/3 1
          2/3 1/3 2/3 1/3 2
          1/3 1/3 1/3 1/3 1/3 1/3 1/3 1/3 1/3 1/3 1/3 1/3
          2/3 1/3 2/3 1/3 2]
        times (reductions + 0 durations)]
      (map note times pitches)))

;row-row-row-your-boat

(defn bpm [beats] (fn [beat] (/ (* beat 60 1000) beats)))
;((bpm 120) 3)

(defn where [k f notes] (->> notes (map #(update-in % [k] f)))) 

;(->> row-row-row-your-boat
;  (where :time (bpm 90))
;  (where :pitch (comp C major))
;  play!)

(defn run [[from & tos]]
  (if-let [to (first tos)]
    (let [up-or-down (if (<= from to)
                       (range from to)
                       (reverse (range (inc to) (inc from))))]
      (concat up-or-down (run tos)))
    [from]))

;(even-melody! (map (comp G major)
;            (run [0 4 -1 1 0])
;            ))

(defn accumulate [series]
  (map (partial sum-n series) (range (count series))))
(def repeats (partial mapcat #(apply repeat %)))
(def runs (partial mapcat run))

(def melody 
  (let [call
          [(repeats [[2 1/4] [1 1/2] [14 1/4] [1 3/2]])
          (runs [[0 -1 3 0] [4] [1 8]])]
        response
          [(repeats [[10 1/4] [1 1/2] [2 1/4] [1 9/4]])
          (runs [[7 -1 0] [0 -3]])]
        development
          [(repeats [[1 3/4] [12 1/4] [1 1/2] [1 1] [1 1/2]
                 [12 1/4] [1 3]])
          (runs [[4] [4] [2 -3] [-1 -2] [0] [3 5] [1] [1] [1 2]
                 [-1 1 -1] [5 0]])]
        [durations pitches]
          (map concat call response development)
        times (map (from 1/2) (accumulate durations))]
      (map note times pitches)))

(def bass
  (let [triples (partial mapcat #(repeat 3 %))]
    (->>
      (map note
        (accumulate (repeats [[21 1] [13 1/4]]))
        (concat
          (triples (runs [[-7 -10] [-12 -10]]))
          (runs [[5 0] [6 0]])))
      (where :part (constantly :bass)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Canon                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canon [f notes] (concat notes (f notes)))

; varieties of canon
(defn simple [wait]
  (fn [notes] (->> notes (where :time (from wait)))))

(defn interval [interval]
  (fn [notes] (->> notes (where :pitch (from interval)))))

(def mirror (fn [notes] (->> notes (where :pitch -))))
(def crab (fn [notes] (->> notes (where :time -))))
(def table (comp mirror crab))

; round
;(->> row-row-row-your-boat
;  (canon (simple 4))
;  (where :pitch (comp C major))
;  (where :time (bpm 90))
;  play!)

; canone alla quarta, by johann sebastian bach
(defn canone-alla-quarta [notes]
  (canon
    (comp (interval -3) mirror (simple 3))
    notes))

;(->> melody 
;  canone-alla-quarta
;  (concat bass)
;  (where :pitch (comp G major))
;  (where :time (bpm 90))
;  play!
;  graph!)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graphing                                                  ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn graph! [notes]
  (let [points (where :time (from (now)) notes)
        highlow (fn [k]
                  (let [values (map k points)]
                    [(apply max values) 
                     (apply min values)]))
        [max-x min-x] (highlow :time)
        [max-y min-y] (highlow :pitch)

        adjust (fn [k high low points]
                 (->> points
                   (where k (from (- low)))
                   (where k #(/ % (- high low)))))
        normalise (fn [points]
                    (->> points
                      (filter #(< (:time %) (now)))
                      (adjust :time max-x min-x)
                      (adjust :pitch max-y min-y)))]
                               
    (sketch 
      :title "Time vs pitch"
      :setup (fn [] (smooth) (frame-rate 20) (background 200))  
      :draw  (fn []
               (let [colours
                       (fnil {:leader 50
                              :follower 100
                              :bass 150}
                             :leader)]
                 (doseq [{x :time y :pitch voice :part}
                         (normalise points)]
                   (stroke-weight 5)
                   (fill 50)
                   (stroke (colours voice))
                   (ellipse
                     (* (width) x)
                     (- (* 2/3 (height)) (* 1/3 (height) y))
                     10 10)))) 
      :size [1024 768])
    notes))
