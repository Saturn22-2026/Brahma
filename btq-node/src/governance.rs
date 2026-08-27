use serde::{Serialize, Deserialize};
use std::collections::HashMap;

#[derive(Serialize, Deserialize, Clone, Debug)]
pub enum ProposalStatus {
    Active,
    Passed,
    Rejected,
    Executed,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct Proposal {
    pub id: u64,
    pub proposer: String,
    pub title: String,
    pub description: String,
    pub start_block: u64,
    pub end_block: u64,
    pub yes_votes: f64,
    pub no_votes: f64,
    pub status: ProposalStatus,
}

pub struct GovernanceEngine {
    pub proposals: HashMap<u64, Proposal>,
    pub next_id: u64,
    pub votes: HashMap<u64, HashMap<String, bool>>, // ProposalID -> (Address -> VoteType)
}

impl GovernanceEngine {
    pub fn new() -> Self {
        GovernanceEngine {
            proposals: HashMap::new(),
            next_id: 0,
            votes: HashMap::new(),
        }
    }

    pub fn submit_proposal(&mut self, proposer: String, title: String, description: String, current_height: u64) -> u64 {
        let id = self.next_id;
        let proposal = Proposal {
            id,
            proposer,
            title,
            description,
            start_block: current_height,
            end_block: current_height + 1000, // 1000 blocks voting period
            yes_votes: 0.0,
            no_votes: 0.0,
            status: ProposalStatus::Active,
        };
        self.proposals.insert(id, proposal);
        self.votes.insert(id, HashMap::new());
        self.next_id += 1;
        id
    }

    pub fn cast_vote(&mut self, proposal_id: u64, voter: String, weight: f64, support: bool) -> Result<(), String> {
        let proposal = self.proposals.get_mut(&proposal_id).ok_or("Proposal not found")?;

        let voter_votes = self.votes.get_mut(&proposal_id).unwrap();
        if voter_votes.contains_key(&voter) {
            return Err("Already voted".to_string());
        }

        if support {
            proposal.yes_votes += weight;
        } else {
            proposal.no_votes += weight;
        }

        voter_votes.insert(voter, support);
        Ok(())
    }

    pub fn update_proposals(&mut self, current_height: u64) {
        for proposal in self.proposals.values_mut() {
            if let ProposalStatus::Active = proposal.status {
                if current_height >= proposal.end_block {
                    if proposal.yes_votes > proposal.no_votes {
                        proposal.status = ProposalStatus::Passed;
                    } else {
                        proposal.status = ProposalStatus::Rejected;
                    }
                }
            }
        }
    }
}
