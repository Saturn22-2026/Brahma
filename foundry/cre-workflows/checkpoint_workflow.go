//go:build wasip1

package main

import (
	"encoding/json"
	"fmt"
	"log/slog"
	"math/big"

	"github.com/ethereum/go-ethereum/common"
	"github.com/smartcontractkit/cre-sdk-go/capabilities/blockchain/evm"
	"github.com/smartcontractkit/cre-sdk-go/capabilities/networking/http"
	"github.com/smartcontractkit/cre-sdk-go/capabilities/scheduler/cron"
	"github.com/smartcontractkit/cre-sdk-go/cre"
	"github.com/smartcontractkit/cre-sdk-go/cre/wasm"
)

type Config struct {
	Schedule       string `json:"schedule"`
	SequencerURL   string `json:"sequencerUrl"`
	TokenAddress   string `json:"tokenAddress"`
	ChainName      string `json:"chainName"`
}

type CheckpointData struct {
	BlockHeight uint64 `json:"blockHeight"`
	MerkleRoot  string `json:"merkleRoot"`
}

func InitWorkflow(config *Config, logger *slog.Logger, secretsProvider cre.SecretsProvider) (cre.Workflow[*Config], error) {
	cronTriggerCfg := &cron.Config{
		Schedule: config.Schedule,
	}

	workflow := cre.Workflow[*Config]{
		cre.Handler(
			cron.Trigger(cronTriggerCfg),
			onCheckpointTrigger,
		),
	}

	return workflow, nil
}

func onCheckpointTrigger(config *Config, runtime cre.Runtime, outputs *cron.Payload) (string, error) {
	logger := runtime.Logger()
	logger.Info("Starting L2 Checkpoint Verification", "sequencer", config.SequencerURL)

	// 1. Fetch latest Merkle Root from Go Sequencer
	httpClient := &http.Client{}
	checkpoint, err := http.SendRequest(config, runtime, httpClient, fetchLatestCheckpoint, cre.ConsensusAggregationFromTags[*CheckpointData]()).Await()
	if err != nil {
		logger.Error("Failed to fetch checkpoint from sequencer", "err", err)
		return "", err
	}

	logger.Info("Fetched Checkpoint", "height", checkpoint.BlockHeight, "root", checkpoint.MerkleRoot)

	// 2. Verify the Merkle Root (Simulated logic: check format)
	if len(checkpoint.MerkleRoot) != 66 { // "0x" + 64 hex
		return "", fmt.Errorf("invalid Merkle Root format: %s", checkpoint.MerkleRoot)
	}

	// 3. Submit Checkpoint to L1 (SovereignToken.sol)
	if err := submitToL1(config, runtime, checkpoint); err != nil {
		logger.Error("Failed to submit checkpoint to L1", "err", err)
		return "", err
	}

	return fmt.Sprintf("Checkpoint Verified and Submitted for Height %d", checkpoint.BlockHeight), nil
}

func fetchLatestCheckpoint(config *Config, logger *slog.Logger, sendRequester *http.SendRequester) (*CheckpointData, error) {
	resp, err := sendRequester.SendRequest(&http.Request{
		Method: "GET",
		Url:    fmt.Sprintf("%s/checkpoint/latest", config.SequencerURL),
	}).Await()
	if err != nil {
		return nil, err
	}

	var checkpoint CheckpointData
	if err := json.Unmarshal(resp.Body, &checkpoint); err != nil {
		return nil, err
	}

	return &checkpoint, nil
}

func submitToL1(config *Config, runtime cre.Runtime, checkpoint *CheckpointData) error {
	logger := runtime.Logger()

	chainSelector, err := evm.ChainSelectorFromName(config.ChainName)
	if err != nil {
		return err
	}

	evmClient := &evm.Client{
		ChainSelector: chainSelector,
	}

	// In a real implementation, we would use generated bindings to call 'submitCheckpoint'
	// For now, we simulate the L1 transaction call
	logger.Info("Submitting checkpoint to L1 contract",
		"contract", config.TokenAddress,
		"height", checkpoint.BlockHeight,
		"root", checkpoint.MerkleRoot)

	return nil
}

func main() {
	wasm.NewRunner(cre.ParseJSON[Config]).Run(InitWorkflow)
}
