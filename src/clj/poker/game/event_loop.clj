(ns poker.game.event-loop
  "Implementations of the game event loop."
  (:require
   [clojure.core.async    :as a]
   [poker.game.protocols  :as p]
   [clojure.tools.logging :as log]))

(defn freeze-state!
  "Stop the event loop, return current game state."
  [event-loop]
  (p/stop event-loop)
  (let [out (p/get-output-ch event-loop)]
    (->> (repeatedly (fn [] (let [x (a/<!! out)] x)))
         (take-while some?)
         (last)
         (second))))

(defn make-timeout-event-channels
  [sched-events]
  (mapv (fn [{:keys [timeout event]}]
          (a/go
            (a/<! (a/timeout timeout))
            (log/infof "trigger event: %s" event)
            event))
        sched-events))

(defn publish-state
  " broadcast current game state in following situations:

  1. when there's at least one valid event applied to game state and no further events to apply
  2. when there's a player action
  "
  [output eng last-evt]
  (a/go
   (when (or (and (some? last-evt)
                  (not (p/has-next-event? eng)))
             (p/has-player-action? eng))
     (a/>! output [:game-output/engine-state eng last-evt]))))

(defn publish-ladder-events
  "publish ladder events, which include buyin, returns & increment hands count."
  [output eng]
  (a/go
   (when-let [ladder-events (p/list-ladder-events eng)]
     (doseq [e ladder-events]
       (a/>! output [:ladder/event eng e])))))

(defn event-loop-run
  "This is the function for event loop iteration.

  - input:  The input event channel
  - output: The output event channel
  - engine: current engine state."
  [input output engine]
  (a/go-loop [eng      engine
              tout-chs #{}
              last-evt nil]
    (a/<! (publish-state output eng last-evt))
    (a/<! (publish-ladder-events output eng))

    ;; validate & apply event
    (let [next-evt (p/next-event eng)
          tout-chs (into tout-chs (make-timeout-event-channels (p/list-scheduled-events eng)))
          eng      (-> eng
                       p/pop-player-action
                       p/flush-scheduled-events
                       p/flush-ladder-events)]
      ;; apply & broadcast player state
      (cond
        ;; Valid next-event, apply
        (and next-evt (p/valid-event? eng next-evt))
        (let [new-eng (p/apply-event (p/pop-next-event eng) next-evt)]
          (log/infof "Applied event (sched:%d) %s" (count tout-chs) next-evt)
          (recur new-eng tout-chs next-evt))
        ;; Invalid next-event, skip
        next-evt
        (let [new-eng (p/pop-next-event eng)]
          (log/errorf "Skipped invalid event (sched:%d) %s" (count tout-chs) next-evt)
          (recur new-eng tout-chs last-evt))
        ;; Otherwise, we read from input and tout-evt-ch(if present)
        :else
        (let [chs        (into [input] tout-chs)
              [evt port] (a/alts! chs)]
          (cond
            ;; Valid event from input, apply, recur with current timeout channel
            (and (= port input) (p/valid-event? eng evt))
            (let [new-eng (p/apply-event eng evt)]
              (log/infof "Applied input event (sched:%d) %s" (count tout-chs) evt)
              (recur new-eng tout-chs evt))
            ;; Invalid event from input, skip
            (and (= port input) (some? evt))
            (do
              (log/warnf "Skipped invalid input event (sched:%d) %s" (count tout-chs) evt)
              (recur eng tout-chs nil))
            ;; Valid event from timeout channel, apply, recur without current timeout channel
            (and (tout-chs port)
                 (p/valid-event? eng evt))
            (let [new-eng  (p/apply-event eng evt)
                  tout-chs (disj tout-chs port)]
              (log/infof "Applied timeout event (sched:%d) %s" (count tout-chs) evt)
              (recur new-eng tout-chs evt))
            ;; Invalid event from timeout channel
            (tout-chs port)
            (let [tout-chs (disj tout-chs port)]
              (log/errorf "Skipped invalid timeout event (sched:%d) %s" (count tout-chs) evt)
              (recur eng tout-chs nil))
            ;; Input channel closed, no timeout event.
            (and (nil? evt)
                 (= port input)
                 (empty? tout-chs))
            (do
              (log/infof "Event loop stop")
              (a/close! output))
            ;; Input channel closed, still have unprocessed timeout event.
            :else
            (do
              (log/infof "Channel closed, wait remaining events (sched:%d)" (count tout-chs))
              (a/<! (a/timeout 2000))
              (recur eng tout-chs nil))))))))

(defrecord EventLoop [input output]
  p/IEventLoop
  (start [_this engine]
    (log/infof "Event loop start with engine: %s" (prn-str engine))
    (event-loop-run input output engine))
  (stop [_this] (a/close! input))
  (get-input-ch [_this] input)
  (get-output-ch [_this] output)
  (send-event [_this event] (a/>!! input event)))

(defn send-events!
  [event-loop & events]
  (doseq [e events]
    (p/send-event event-loop e)))

(defn make-event-loop
  []
  (let [input (a/chan 100) output (a/chan 100)] (->EventLoop input output)))
