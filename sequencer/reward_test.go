package main

import (
	"testing"
)

func TestCalculateBlockReward(t *testing.T) {
	initial := 50.0
	r0 := CalculateBlockReward(0, initial)
	if r0 != initial {
		t.Errorf("Expected initial reward %f, got %f", initial, r0)
	}

	r100 := CalculateBlockReward(100, initial)
	if r100 >= initial {
		t.Errorf("Reward should decay over time. r100: %f", r100)
	}

	// Test tail emission floor
	rExtremelyLate := CalculateBlockReward(1000000, initial)
	if rExtremelyLate < 0.01 {
		t.Errorf("Tail emission floor not enforced. got %f", rExtremelyLate)
	}
}

func TestAdjustDifficultyEMA(t *testing.T) {
	target := 60.0
	current := 1.0

	// Scenario 1: Solve time is too fast -> Difficulty should increase
	highDiff := AdjustDifficultyEMA(30.0, target, current)
	if highDiff <= current {
		t.Errorf("Difficulty should increase when solve time is fast. got %f", highDiff)
	}

	// Scenario 2: Solve time is too slow -> Difficulty should decrease
	lowDiff := AdjustDifficultyEMA(120.0, target, current)
	if lowDiff >= current {
		t.Errorf("Difficulty should decrease when solve time is slow. got %f", lowDiff)
	}
}
