package cn.nukkit.utils.serverconfig.category;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import org.cloudburstmc.netty.channel.raknet.RakConstants;

import java.util.List;

/**
 * Network configuration record (Java 17+).
 * Provides immutable data container with automatic equals, hashCode, and toString.
 */
public record NetworkSettings(
    @Comment("ZLIB compression provider (2 recommended)")
    @CustomKey("zlib-provider")
    int zlibProvider,
    
    @Comment("Compression level (1-9, higher = more CPU, smaller packets)")
    @CustomKey("compression-level")
    int compressionLevel,
    
    @Comment("Chunk compression level (1-9, higher = more CPU, smaller chunks)")
    @CustomKey("chunk-compression-level")
    int chunkCompressionLevel,
    
    @Comment("Compression threshold in bytes")
    @CustomKey("compression-threshold")
    int compressionThreshold,
    
    @Comment("Use Snappy compression instead of ZLIB")
    @CustomKey("use-snappy-compression")
    boolean useSnappyCompression,
    
    @Comment("RakNet packet limit per tick")
    @CustomKey("rak-packet-limit")
    int rakPacketLimit,
    
    @Comment("RakNet cookie mode (active, offloaded, offloaded_psk, off, none, stateless)")
    @CustomKey("rak-cookie-mode")
    String rakCookieMode,
    
    @Comment("Client timeout in milliseconds (reserved, not yet applied)")
    @CustomKey("timeout-milliseconds")
    int timeoutMilliseconds,
    
    @Comment("Show plugin list in query response")
    @CustomKey("query-plugins")
    boolean queryPlugins,
    
    @Comment("Enable WaterDog proxy mode")
    @CustomKey("use-waterdog")
    boolean useWaterdog,
    
    @Comment("ViaProxy Java Edition player username prefix")
    @CustomKey("viaproxy-username-prefix")
    String viaProxyUsernamePrefix,
    
    @Comment("Enable Proxy Protocol v2 for UDP proxies (e.g. FRP). Whitelisted sources must send a valid PPv2 header; non-whitelisted sources are treated as direct clients")
    @CustomKey("enable-proxy-protocol")
    boolean enableProxyProtocol,
    
    @Comment("Whitelisted proxy source IP/CIDR entries for Proxy Protocol. Use proxy addresses, not player addresses. Headerless or invalid packets from whitelisted sources are dropped")
    @CustomKey("proxy-protocol-whitelist")
    List<String> proxyProtocolWhitelist
) {
    // Canonical constructor with defaults
    public NetworkSettings {
        if (rakCookieMode == null || rakCookieMode.isBlank()) {
            rakCookieMode = "active";
        }
        if (viaProxyUsernamePrefix == null) {
            viaProxyUsernamePrefix = "";
        }
        if (proxyProtocolWhitelist == null) {
            proxyProtocolWhitelist = List.of("127.0.0.1/32");
        }
    }
    
    // Convenience constructor with defaults
    public NetworkSettings() {
        this(2, 5, 7, 256, false, RakConstants.DEFAULT_PACKET_LIMIT, 
             "active", 25000, false, false, "", false, List.of("127.0.0.1/32"));
    }
    
    // Builder-style withers for immutable updates
    public NetworkSettings withZlibProvider(int zlibProvider) {
        return new NetworkSettings(zlibProvider, this.compressionLevel, this.chunkCompressionLevel,
                this.compressionThreshold, this.useSnappyCompression, this.rakPacketLimit,
                this.rakCookieMode, this.timeoutMilliseconds, this.queryPlugins, this.useWaterdog,
                this.viaProxyUsernamePrefix, this.enableProxyProtocol, this.proxyProtocolWhitelist);
    }
    
    public NetworkSettings withCompressionLevel(int compressionLevel) {
        return new NetworkSettings(this.zlibProvider, compressionLevel, this.chunkCompressionLevel,
                this.compressionThreshold, this.useSnappyCompression, this.rakPacketLimit,
                this.rakCookieMode, this.timeoutMilliseconds, this.queryPlugins, this.useWaterdog,
                this.viaProxyUsernamePrefix, this.enableProxyProtocol, this.proxyProtocolWhitelist);
    }
    
    public NetworkSettings withRakCookieMode(String rakCookieMode) {
        return new NetworkSettings(this.zlibProvider, this.compressionLevel, this.chunkCompressionLevel,
                this.compressionThreshold, this.useSnappyCompression, this.rakPacketLimit,
                rakCookieMode != null ? rakCookieMode : "active", this.timeoutMilliseconds, 
                this.queryPlugins, this.useWaterdog, this.viaProxyUsernamePrefix, 
                this.enableProxyProtocol, this.proxyProtocolWhitelist);
    }
}
