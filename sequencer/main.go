package main

import (
	"fmt"
)

func main() {
	fmt.Println("=== Starting Bitcoin-Quantum L2 Sequencer (Go) ===")

	// Initialize Engine: 50 QTM initial reward, 60s target block time
	engine := NewSequencerEngine(50.0, 60.0)

	// Simulate 10 blocks with steady hashpower
	fmt.Println("\n--- Normal Network Conditions ---")
	for i := 0; i < 5; i++ {
		stats := engine.MineL2Block(1.0)
		fmt.Printf("Block %d: Reward: %.4f | Solve Time: %.2fs | Difficulty: %.4f\n",
			stats["height"], stats["reward"], stats["solve_time"], stats["next_difficulty"])
	}

	// Simulate sudden hashpower surge
	fmt.Println("\n--- Hashpower Surge (Difficulty should rise) ---")
	for i := 0; i < 5; i++ {
		stats := engine.MineL2Block(10.0)
		fmt.Printf("Block %d: Reward: %.4f | Solve Time: %.2fs | Difficulty: %.4f\n",
			stats["height"], stats["reward"], stats["solve_time"], stats["next_difficulty"])
	}

	fmt.Println("\n=== Final L2 Sequencer State ===")
	engine.PrintStats()
}
