use std::error::Error;
use async_trait::async_trait;

#[async_trait]
pub trait HardwareDriver: Send + Sync {
    async fn open(&mut self) -> Result<(), Box<dyn Error + Send + Sync>>;
    async fn close(&mut self) -> Result<(), Box<dyn Error + Send + Sync>>;
    async fn write_raw(&self, buf: &[u8]) -> Result<(), Box<dyn Error + Send + Sync>>;
    async fn read_raw(&self) -> Result<Vec<u8>, Box<dyn Error + Send + Sync>>;
}

pub struct HALManager {
    driver: Option<Box<dyn HardwareDriver>>,
}

impl HALManager {
    pub fn new() -> Self {
        Self { driver: None }
    }

    pub fn set_driver(&mut self, driver: Box<dyn HardwareDriver>) {
        self.driver = Some(driver);
    }

    pub async fn transmit(&self, data: &[u8]) -> Result<(), Box<dyn Error + Send + Sync>> {
        if let Some(ref driver) = self.driver {
            driver.write_raw(data).await
        } else {
            Err("No hardware driver attached".into())
        }
    }

    pub async fn receive(&self) -> Result<Vec<u8>, Box<dyn Error + Send + Sync>> {
        if let Some(ref driver) = self.driver {
            driver.read_raw().await
        } else {
            Err("No hardware driver attached".into())
        }
    }
}

// --- Implementations ---

pub struct MockHAL;
#[async_trait]
impl HardwareDriver for MockHAL {
    async fn open(&mut self) -> Result<(), Box<dyn Error + Send + Sync>> {
        println!("[MockHAL] Device opened.");
        Ok(())
    }
    async fn close(&mut self) -> Result<(), Box<dyn Error + Send + Sync>> {
        println!("[MockHAL] Device closed.");
        Ok(())
    }
    async fn write_raw(&self, buf: &[u8]) -> Result<(), Box<dyn Error + Send + Sync>> {
        println!("[MockHAL] Writing {} bytes: {:?}", buf.len(), buf);
        Ok(())
    }
    async fn read_raw(&self) -> Result<Vec<u8>, Box<dyn Error + Send + Sync>> {
        Ok(vec![0x42, 0x43])
    }
}

pub struct SerialHAL {
    pub port: String,
    pub baud_rate: u32,
}
#[async_trait]
impl HardwareDriver for SerialHAL {
    async fn open(&mut self) -> Result<(), Box<dyn Error + Send + Sync>> {
        println!("[SerialHAL] Opening port {} at {} baud.", self.port, self.baud_rate);
        Ok(())
    }
    async fn close(&mut self) -> Result<(), Box<dyn Error + Send + Sync>> {
        Ok(())
    }
    async fn write_raw(&self, buf: &[u8]) -> Result<(), Box<dyn Error + Send + Sync>> {
        println!("[SerialHAL] Sending {} bytes over SDR/PLC serial link.", buf.len());
        Ok(())
    }
    async fn read_raw(&self) -> Result<Vec<u8>, Box<dyn Error + Send + Sync>> {
        Ok(vec![])
    }
}
