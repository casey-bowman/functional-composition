(ns overtunes.songs.before
  (:use [overtone.live])
  (:use [overtunes.core])
  (:use [overtunes.instruments.organ-cornet]))

(def melody
  "A crude five-note melody that will constantly fall out of time when played
  over chords at a rate of two notes per chord." 
  [[:C3 :Bb3 :Eb3 :G3 :Eb3]
   [1/1 1/1  1/1  1/1 1/1]])

(def start
  "In which we establish the key and give the syncopated relationship between
  the melody and the chords time to become apparent." 
  [(chord :Eb3 :major)
   (chord :Eb3 :major)
   (chord :C3  :minor7)
   (chord :C3  :minor7)
   (chord :Eb3 :major)
   (chord :Eb3 :major)
   (chord :C3  :minor7)
   (chord :C3  :minor7)])

(def middle
  "In which we let the melody combine in interesting ways with a developing
  progression that builds and then resolves."
  [(chord :G3  :minor)
   (chord :G3  :minor)
   (chord :Ab3 :major)
   (chord :Bb3 :major)
   (chord :Eb4 :major)
   (chord :Eb4 :major)
   (chord :Bb3 :major)
   (chord :Bb3 :major)])

(def variation
  "In which we elaborate on the middle section."
  [(chord :G3  :minor)
   (chord :G3  :minor)
   (chord :Ab3 :major)
   (chord :Ab3 :major)
   (chord :G3  :minor)
   (chord :G3  :minor)
   (chord :Ab3 :major)
   (chord :Ab3 :major)
   (chord :G3  :minor)
   (chord :G3  :minor)
   (chord :Ab3 :major)
   (chord :Bb3 :major)
   (chord :Eb4 :major)
   (chord :Eb4 :major)
   (chord :Bb3 :major)
   (chord :Bb3 :major)])

(def finish
  "In which we return to root for resolution at the end of the piece."
  [(chord :Eb4 :major)
   (chord :Eb4 :major)])

(defn play-progression [progression metro]
  "Plays a seq of chords for two beats each on the cornet.
  Takes a relative metronome in addition to the chord progression.
  (play-progression [root fourth fifth] metro)"
  (let [beats-per-chord 2
        duration (* beats-per-chord (beat-length metro))]
    (when-not (empty? progression)
      (at (metro) (play-chord (first progression) organ-cornet duration))
      (play-progression (rest progression)
        (metronome-from metro beats-per-chord)))))

(defn cycle-n
  "Returns a new seq which is cycled n times.
  (cycle-n 2 [1 2 3]) ;=> [1 2 3 1 2 3]
  (cycle-n 2 [[1 2 3][4 5 6]]) ;=> [[1 2 3 1 2 3][4 5 6 4 5 6]]"
  [n s]
  (map concat (take (* (count s) n) (cycle s))))

(defn play
  "Play the melody over the chords to the relative metro's time."
  [chords metro]
  (let [repetitions-per-chord (/ (count melody) 2) 
        melody-line (cycle-n (/ (count chords) repetitions-per-chord) melody)] 
  (play-melody melody-line organ-cornet metro)
  (play-progression (concat chords finish) metro)))

(defn before-full []
  "The full version of 'Before', grave."
  (play (concat start middle start middle variation) (metronome 30)))

(defn before-short []
  "A short version of 'Before', adante."
  (play variation (metronome 100)))
