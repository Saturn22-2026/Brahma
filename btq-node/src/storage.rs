use rocksdb::{DB, Options, WriteBatch};
use serde_json;
use crate::Block;
use std::path::Path;

/**
 * @title BTQStorage
 * @dev High-performance persistent storage for the BTQ blockchain using RocksDB.
 */
pub struct BTQStorage {
    db: DB,
}

impl BTQStorage {
    /**
     * @dev Opens the database at the specified path.
     */
    pub fn open<P: AsRef<Path>>(path: P) -> Self {
        let mut opts = Options::default();
        opts.create_if_missing(true);
        // Optimize for blockchain throughput
        opts.set_max_background_jobs(4);
        opts.set_write_buffer_size(64 * 1024 * 1024); // 64MB

        let db = DB::open(&opts, path).expect("Failed to open RocksDB");
        BTQStorage { db }
    }

    /**
     * @dev Persists a block and updates the height/hash indexes atomically.
     */
    pub fn put_block(&self, block: &Block) -> Result<(), String> {
        let block_json = serde_json::to_string(block).map_err(|e| e.to_string())?;
        let hash = block.calculate_hash();

        let mut batch = WriteBatch::default();

        // 1. Index by Height: "height:0" -> BlockData
        let height_key = format!("height:{}", block.index);
        batch.put(height_key.as_bytes(), block_json.as_bytes());

        // 2. Index by Hash: "hash:0x..." -> Height
        let hash_key = format!("hash:{}", hash);
        batch.put(hash_key.as_bytes(), block.index.to_be_bytes());

        // 3. Update Latest Height
        batch.put(b"latest_height", block.index.to_be_bytes());

        self.db.write(batch).map_err(|e| e.to_string())
    }

    /**
     * @dev Retrieves a block by its height.
     */
    pub fn get_block_by_height(&self, height: u64) -> Option<Block> {
        let key = format!("height:{}", height);
        match self.db.get(key.as_bytes()).ok().flatten() {
            Some(data) => serde_json::from_slice(&data).ok(),
            None => None,
        }
    }

    /**
     * @dev Gets the latest block height recorded in the database.
     */
    pub fn get_latest_height(&self) -> u64 {
        match self.db.get(b"latest_height").ok().flatten() {
            Some(data) => {
                let mut bytes = [0u8; 8];
                bytes.copy_from_slice(&data);
                u64::from_be_bytes(bytes)
            },
            None => 0,
        }
    }
}
