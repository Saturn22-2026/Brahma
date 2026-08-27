//go:build wasip1

package main

import (
	"encoding/json"
	"fmt"
	"log/slog"

	"github.com/smartcontractkit/cre-sdk-go/capabilities/networking/http"
	"github.com/smartcontractkit/cre-sdk-go/capabilities/scheduler/cron"
	"github.com/smartcontractkit/cre-sdk-go/cre"
	"github.com/smartcontractkit/cre-sdk-go/cre/wasm"
)

type Config struct {
	Schedule       string `json:"schedule"`
	TokenAddress   string `json:"tokenAddress"`
	RelayURL       string `json:"relayUrl"`
}

type MarketStats struct {
	CurrentPrice float64 `json:"current_price"`
}

func InitWorkflow(config *Config, logger *slog.Logger, secretsProvider cre.SecretsProvider) (cre.Workflow[*Config], error) {
	cronTriggerCfg := &cron.Config{
		Schedule: config.Schedule,
	}

	workflow := cre.Workflow[*Config]{
		cre.Handler(
			cron.Trigger(cronTriggerCfg),
			onStabilizationCheck,
		),
	}

	return workflow, nil
}

func onStabilizationCheck(config *Config, runtime cre.Runtime, outputs *cron.Payload) (string, error) {
	logger := runtime.Logger()
	logger.Info("Checking Sovereign Stability", "relay", config.RelayURL)

	// 1. Fetch real-time market stats from the Central Relay
	httpClient := &http.Client{}
	stats, err := http.SendRequest(config, runtime, httpClient, fetchMarketStats, cre.ConsensusAggregationFromTags[*MarketStats]()).Await()
	if err != nil {
		logger.Error("Failed to fetch market stats", "err", err)
		return "", err
	}

	logger.Info("Market Price", "price", stats.CurrentPrice)

	// 2. Cross-verify with Price Floor
	priceFloor := 0.85
	if stats.CurrentPrice < priceFloor {
		logger.Warn("Price Stability Warning! Triggering external coordinator.")
		// In a real system, this could alert the Reserve Managers via encrypted notification
	}

	return fmt.Sprintf("Stability Check Complete. Price: %.4f", stats.CurrentPrice), nil
}

func fetchMarketStats(config *Config, logger *slog.Logger, sendRequester *http.SendRequester) (*MarketStats, error) {
	resp, err := sendRequester.SendRequest(&http.Request{
		Method: "GET",
		Url:    fmt.Sprintf("%s/economy/stats", config.RelayURL),
	}).Await()
	if err != nil {
		return nil, err
	}

	var stats MarketStats
	if err := json.Unmarshal(resp.Body, &stats); err != nil {
		return nil, err
	}

	return &stats, nil
}

func main() {
	wasm.NewRunner(cre.ParseJSON[Config]).Run(InitWorkflow)
}
