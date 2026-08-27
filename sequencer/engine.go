package main

import (
	"fmt"
)

// SequencerEngine tracks the L2 state and manages block production.
type SequencerEngine struct {
	BlockHeight       uint64
	CurrentDifficulty float64
	TargetBlockTime   float64
	InitialReward     float64
	TotalEmitted      float64
}

// NewSequencerEngine initializes a new L2 engine.
func NewSequencerEngine(initialReward, targetBlockTime float64) *SequencerEngine {
	return &SequencerEngine{
		BlockHeight:       0,
		CurrentDifficulty: 1.0,
		TargetBlockTime:   targetBlockTime,
		InitialReward:     initialReward,
		TotalEmitted:      0,
	}
}

// MineL2Block simulates the production of a new L2 block.
func (e *SequencerEngine) MineL2Block(minerHashpower float64) map[string]interface{} {
	e.BlockHeight++

	// 1. Calculate Reward
	reward := CalculateBlockReward(e.BlockHeight, e.InitialReward)
	e.TotalEmitted += reward

	// 2. Simulate Block solve time (Hashpower vs Difficulty)
	baseSolveTime := e.TargetBlockTime * 2
	actualBlockTime := baseSolveTime * (e.CurrentDifficulty / mathMax(minerHashpower, 0.1))

	// 3. Adjust Difficulty
	e.CurrentDifficulty = AdjustDifficultyEMA(actualBlockTime, e.TargetBlockTime, e.CurrentDifficulty)

	return map[string]interface{}{
		"height":            e.BlockHeight,
		"reward":            reward,
		"solve_time":        actualBlockTime,
		"next_difficulty":   e.CurrentDifficulty,
		"total_emitted":     e.TotalEmitted,
	}
}

func mathMax(a, b float64) float64 {
	if a > b {
		return a
	}
	return b
}

func (e *SequencerEngine) PrintStats() {
	fmt.Printf("[L2 Sequencer] Height: %d | Price: (Oracle) | Reward: %.4f | Difficulty: %.4f | Emitted: %.2f\n",
		e.BlockHeight, CalculateBlockReward(e.BlockHeight, e.InitialReward), e.CurrentDifficulty, e.TotalEmitted)
}
