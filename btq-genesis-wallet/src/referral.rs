// src/referral.rs
pub struct ReferralSystem {
    pub base_reward: f64,
    pub tier_percentages: Vec<f64>, // e.g., [0.10, 0.05, 0.02] for 10%, 5%, 2%
}

impl ReferralSystem {
    pub fn calculate_rewards(&self, generated_revenue: f64, depth: usize) -> f64 {
        if depth == 0 || depth > self.tier_percentages.len() {
            return 0.0;
        }
        generated_revenue * self.tier_percentages[depth - 1]
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_tier_one_referral_math() {
        let system = ReferralSystem {
            base_reward: 100.0,
            tier_percentages: vec![0.10, 0.05, 0.02],
        };
        let reward = system.calculate_rewards(1000.0, 1);
        assert_eq!(reward, 100.0); // 10% of 1000
    }

    #[test]
    fn test_out_of_bounds_depth() {
        let system = ReferralSystem {
            base_reward: 100.0,
            tier_percentages: vec![0.10, 0.05],
        };
        let reward = system.calculate_rewards(1000.0, 3);
        assert_eq!(reward, 0.0); // Depth 3 doesn't exist
    }
}
