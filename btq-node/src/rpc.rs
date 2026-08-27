use jsonrpsee::proc_macros::rpc;
use jsonrpsee::core::RpcResult;
use std::sync::Arc;
use tokio::sync::RwLock;
use crate::BlockchainCore;
use serde::{Serialize, Deserialize};

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct NetworkStats {
    pub chain_height: u64,
    pub total_mined: f64,
    pub difficulty: usize,
    pub p2p_status: String,
}

#[rpc(server)]
pub trait BTQApi {
    #[method(name = "btq_getNetworkStats")]
    async fn get_network_stats(&self) -> RpcResult<NetworkStats>;

    #[method(name = "btq_getLatestBlock")]
    async fn get_latest_block(&self) -> RpcResult<String>;
}

pub struct BTQApiServerImpl {
    pub core: Arc<RwLock<BlockchainCore>>,
}

#[async_trait::async_trait]
impl BTQApiServer for BTQApiServerImpl {
    async fn get_network_stats(&self) -> RpcResult<NetworkStats> {
        let core = self.core.read().await;
        Ok(NetworkStats {
            chain_height: core.last_block.index,
            total_mined: core.total_mined,
            difficulty: core.difficulty,
            p2p_status: "CONNECTED".to_string(),
        })
    }

    async fn get_latest_block(&self) -> RpcResult<String> {
        let core = self.core.read().await;
        Ok(serde_json::to_string(&core.last_block).unwrap())
    }
}
