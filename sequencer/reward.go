package main

import (
	"math"
)

// CalculateBlockReward implements the continuous asymptotic emission decay.
// Formula: R = R0 * exp(-lambda * h)
func CalculateBlockReward(blockHeight uint64, initialReward float64) float64 {
	decayConstant := 0.0001
	// Continuous decay instead of Bitcoin halving
	reward := initialReward * math.Exp(-decayConstant*float64(blockHeight))
	if reward < 0.01 {
		return 0.01 // Tail emission for perpetual security
	}
	return reward
}

// AdjustDifficultyEMA adjusts difficulty EVERY BLOCK using Exponential Moving Average (EMA).
func AdjustDifficultyEMA(actualTime, targetTime, currentDifficulty float64) float64 {
	alpha := 0.1 // Smoothing factor
	// If actual time < target time, network is too fast -> increase difficulty
	// If actual time > target time, network is too slow -> decrease difficulty
	ratio := actualTime / targetTime
	newDifficulty := (currentDifficulty * (1 - alpha)) + (currentDifficulty * ratio * alpha)

	// Prevent difficulty from dropping too low
	if newDifficulty < 0.001 {
		return 0.001
	}
	return newDifficulty
}
