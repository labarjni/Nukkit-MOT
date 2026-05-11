package cn.nukkit.utils.serverconfig.category;

import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;

/**
 * Debug configuration record (Java 17+).
 * Provides immutable data container with automatic equals, hashCode, and toString.
 */
public record DebugSettings(
    @Comment("Debug level (1=errors only, 2=warnings, 3=info)")
    @CustomKey("debug-level")
    int debugLevel,
    
    @Comment("Enable ANSI colors in terminal title")
    @CustomKey("ansi-title")
    boolean ansiTitle,
    
    @Comment("Show verbose deprecation warnings")
    @CustomKey("deprecated-verbose")
    boolean deprecatedVerbose,
    
    @Comment("Call DataPacketSendEvent")
    @CustomKey("call-data-pk-send-event")
    boolean callDataPkSendEvent,
    
    @Comment("Call BatchPacketSendEvent")
    @CustomKey("call-batch-pk-send-event")
    boolean callBatchPkSendEvent,
    
    @Comment("Call EntityMotionEvent")
    @CustomKey("call-entity-motion-event")
    boolean callEntityMotionEvent,
    
    @Comment("Enable block listener")
    @CustomKey("block-listener")
    boolean blockListener,
    
    @Comment("Enable automatic bug reporting (Sentry)")
    @CustomKey("automatic-bug-report")
    boolean automaticBugReport,
    
    @Comment("Show update notifications")
    @CustomKey("update-notifications")
    boolean updateNotifications,
    
    @Comment("Enable bStats metrics")
    @CustomKey("bstats-metrics")
    boolean bstatsMetrics,
    
    @Comment("Hastebin API token for paste uploads")
    @CustomKey("hastebin-token")
    String hastebinToken
) {
    // Canonical constructor with defaults
    public DebugSettings {
        if (hastebinToken == null) {
            hastebinToken = "";
        }
    }
    
    // Convenience constructor with defaults
    public DebugSettings() {
        this(1, false, true, true, true, true, true, true, true, true, "");
    }
    
    // Builder-style withers for immutable updates
    public DebugSettings withDebugLevel(int debugLevel) {
        return new DebugSettings(debugLevel, this.ansiTitle, this.deprecatedVerbose, this.callDataPkSendEvent,
                this.callBatchPkSendEvent, this.callEntityMotionEvent, this.blockListener, 
                this.automaticBugReport, this.updateNotifications, this.bstatsMetrics, this.hastebinToken);
    }
    
    public DebugSettings withAutomaticBugReport(boolean automaticBugReport) {
        return new DebugSettings(this.debugLevel, this.ansiTitle, this.deprecatedVerbose, this.callDataPkSendEvent,
                this.callBatchPkSendEvent, this.callEntityMotionEvent, this.blockListener, 
                automaticBugReport, this.updateNotifications, this.bstatsMetrics, this.hastebinToken);
    }
}
