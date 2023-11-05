(ns test-entity 
  (:require [clojure.test :refer [deftest is testing]]))

(deftest name-test
  (testing "Context of the test assertions"
    (is (= 1 2))))

(deftest name-test-2
  (testing "Context of the test assertions 2"
    (is (= 1 2))))
