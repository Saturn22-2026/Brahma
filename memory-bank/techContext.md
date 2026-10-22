# Tech context

**Stack**

- Language / runtime: Python 3 L1, Kotlin / JVM 17 wallet, Java JBR 17 + **BouncyCastle 1.72** for Dilithium verify (wallet signs 3293-byte Dilithium3; BC 1.78.1 is incompatible — use `btq-node/bcprov-jdk18on-1.72.jar` / `repair_windows_signature_checker.ps1`)
- Framework: Android Gradle Plugin + Jetpack Compose
- Package manager: pip (`btq-node/requirements.txt`, `polymarket-hft/requirements.txt`), Gradle (`wallet-android/`)

**Environment**

- Node / Python / etc. versions: JDK 17 (local install `C:\Users\Navesh\.jdks\jbr-17.0.14`; Gradle uses `JAVA_HOME` / foojay toolchains — do not commit `org.gradle.java.home`), Android SDK at `C:\Users\Navesh\AppData\Local\Android\Sdk`
- Required env vars (names only; no secrets): `ANDROID_HOME` / `sdk.dir` in `local.properties`
- L1: `BRAH_DATA_DIR`, `BRAH_BIND` (default `127.0.0.1`), `BRAH_ALLOW_LAN`, `BRAH_JOIN_MAX_PER_HOUR`, `BRAH_P2P_PORT`, `BRAH_RPC_PORT`, `BRAH_UDP_PORT`, `BRAH_PEERS`, `BRAH_PUBLIC_RPC`, `BRAH_CORS_ORIGIN`, `BRAH_EXPLORER`, `BRAH_TLS_CERT`, `BRAH_TLS_KEY`, `BRAH_SERVE_APK`, `BRAH_JAVA`, `BRAH_BCPROV_JAR`, `BRAH_TUNNEL_CONFIG`, `BRAH_TUNNEL_TOKEN`
- Public host (bake-in): `https://www.brahmnetwork.com` via `btq-data/public_rpc.txt` and `wallet-android/local.properties` `brah.publicRpc=`
- Cloudflare Public Hostname API (optional, never commit): `CF_API_TOKEN`, `CF_ACCOUNT_ID`, `CF_TUNNEL_ID`, `CF_HOSTNAME`, `CF_ZONE_NAME` / `CF_ZONE_ID`. Tunnel origin is `http://127.0.0.1:8545`, never the public URL.

**Build & test**

- Commands:
  - Wallet: from `wallet-android/` with `GRADLE_USER_HOME=C:\Users\Navesh\.gradle`: `.\gradlew.bat :app:assembleDebug --no-daemon` then `adb -s R92X30JKZTY install -r app\build\outputs\apk\debug\app-debug.apk`
  - L1 gates: `cd btq-node` then `python test_launch_gates.py`
  - Public TLS watch: `cd btq-node` then `python check_public_tls.py` (exit 2 if cert <21 days). Windows task: `install_tls_watch_task.ps1`. Linux timer: `install_tls_watch_linux.sh`. Cloudflare Universal SSL is the renewer; optional `CF_API_TOKEN` + `CF_ZONE_NAME` re-asserts Full (strict) / Always HTTPS / HSTS. Do not upload a 2100-dated cert.
  - Polymarket bot: `cd polymarket-hft` then `pytest` and `python -m app` (localhost 8787 only)

**Constraints**

- Wallet requires INTERNET. Debug builds allow LAN HTTP RPC; release disables cleartext. Location / nearby-Wi-Fi for Sentinel Wi-Fi Aware. Attached device: Samsung SM-A055F (`R92X30JKZTY`).
- Multi-node L1: set `BRAH_PEERS=host:18545` (and unique `BRAH_P2P_PORT` if two nodes share a host).
- Brand: coin Brahma, network BRAHMNETWORK, host www.brahmnetwork.com. Package `com.brahmnetwork.wallet`. Canonical RPC `brah_*` (`brm_*`/`btq_*` aliases). Env `BRAH_*`→`BRM_*`→`BTQ_*`. See `docs/BRM_RENAME.md`.
