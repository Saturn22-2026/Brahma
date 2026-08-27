use libp2p::{
    gossipsub,
    noise,
    tcp,
    yamux,
    core::upgrade,
    swarm::{SwarmEvent, SwarmBuilder},
    Multiaddr,
    PeerId,
    Transport,
};
use futures::StreamExt;
use std::error::Error;
use std::time::Duration;
use tokio::time::sleep;

/**
 * @title BTQChaosMonkey
 * @dev Malicious node simulator for stress testing the BTQ network resilience.
 */
#[tokio::main]
async fn main() -> Result<(), Box<dyn Error>> {
    println!("=== BTQ CHAOS MONKEY: ADVERSARIAL NODE STARTING ===");

    // 1. Generate Identity
    let local_key = libp2p::identity::Keypair::generate_ed25519();
    let local_peer_id = PeerId::from(local_key.public());
    println!("[Chaos] Malicious PeerID: {}", local_peer_id);

    // 2. Setup Gossipsub (Signed)
    let gossipsub_config = gossipsub::ConfigBuilder::default()
        .heartbeat_interval(Duration::from_secs(1))
        .build()?;

    let mut gossipsub = gossipsub::Behaviour::new(
        gossipsub::MessageAuthenticity::Signed(local_key.clone()),
        gossipsub_config,
    )?;

    let block_topic = gossipsub::IdentTopic::new("btq/blocks/1.0");
    gossipsub.subscribe(&block_topic)?;

    // 3. Build Swarm
    let mut swarm = SwarmBuilder::with_existing_identity(local_key)
        .with_tokio()
        .with_tcp(
            tcp::Config::default(),
            noise::Config::new,
            yamux::Config::default,
        )?
        .with_behaviour(|_| gossipsub)?
        .build();

    // 4. Connect to local production node
    let target_addr: Multiaddr = "/ip4/127.0.0.1/tcp/1337".parse()?;
    swarm.dial(target_addr)?;
    println!("[Chaos] Dialing production node at /ip4/127.0.0.1/tcp/1337...");

    // 5. Chaos Loop
    println!("[Chaos] Initiating Spam Protocol...");
    let mut counter = 0;

    loop {
        tokio::select! {
            _ = sleep(Duration::from_millis(100)) => {
                // Flood with "Malformed" Block Data
                let junk_data = format!("MALFORMED_BLOCK_SURPRISE_{}", counter);
                if let Err(e) = swarm.behaviour_mut().publish(block_topic.clone(), junk_data.as_bytes()) {
                    println!("[Chaos] Publish failed (likely blacklisted): {:?}", e);
                } else {
                    println!("[Chaos] Injected junk block #{}", counter);
                }
                counter += 1;
            }
            event = swarm.select_next_some() => match event {
                SwarmEvent::ConnectionEstablished { peer_id, .. } => {
                    println!("[Chaos] Connected to victim: {}", peer_id);
                }
                SwarmEvent::OutgoingConnectionError { error, .. } => {
                    println!("[Chaos] Connection rejected: {:?}", error);
                }
                _ => {}
            }
        }
    }
}
