package cn.nukkit.command.defaults;

import cn.nukkit.Nukkit;
import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.level.Level;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.network.Network;
import cn.nukkit.utils.SystemMetrics;
import cn.nukkit.utils.TextFormat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2015/11/11 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class StatusCommand extends VanillaCommand {

    private static final String UPTIME_FORMAT = TextFormat.RED + "%d" + TextFormat.GOLD + " days " +
            TextFormat.RED + "%d" + TextFormat.GOLD + " hours " +
            TextFormat.RED + "%d" + TextFormat.GOLD + " minutes " +
            TextFormat.RED + "%d" + TextFormat.GOLD + " seconds";

    private static final Map<String, String> vmVendor = new HashMap<>(10, 0.99f);
    private static final Map<String, String> vmMac = new HashMap<>(10, 0.99f);
    private static final String[] vmModelArray = new String[]{"Linux KVM", "Linux lguest", "OpenVZ", "Qemu",
            "Microsoft Virtual PC", "VMWare", "linux-vserver", "Xen", "FreeBSD Jail", "VirtualBox", "Parallels",
            "Linux Containers", "LXC", "Bochs"};

    static {
        vmVendor.put("bhyve", "bhyve");
        vmVendor.put("KVM", "KVM");
        vmVendor.put("TCG", "QEMU");
        vmVendor.put("Microsoft Hv", "Microsoft Hyper-V or Windows Virtual PC");
        vmVendor.put("lrpepyh vr", "Parallels");
        vmVendor.put("VMware", "VMware");
        vmVendor.put("XenVM", "Xen HVM");
        vmVendor.put("ACRN", "Project ACRN");
        vmVendor.put("QNXQVMBSQG", "QNX Hypervisor");

        vmMac.put("00:50:56", "VMware ESX 3");
        vmMac.put("00:0C:29", "VMware ESX 3");
        vmMac.put("00:05:69", "VMware ESX 3");
        vmMac.put("00:03:FF", "Microsoft Hyper-V");
        vmMac.put("00:1C:42", "Parallels Desktop");
        vmMac.put("00:0F:4B", "Virtual Iron 4");
        vmMac.put("00:16:3E", "Xen or Oracle VM");
        vmMac.put("08:00:27", "VirtualBox");
        vmMac.put("02:42:AC", "Docker Container");
    }

    public StatusCommand(String name) {
        super(name, "%nukkit.command.status.description", "%nukkit.command.status.usage");
        this.setPermission("nukkit.command.status");
        this.commandParameters.clear();
        this.addCommandParameters("default", new CommandParameter[]{
                CommandParameter.newEnum("mode", true, new String[]{"full", "simple"})
        });
    }

    private static String formatKB(double bytes) {
        return NukkitMath.round((bytes / 1024 * 1000), 2) + " KB";
    }

    private static String formatKB(long bytes) {
        return NukkitMath.round((bytes / 1024d * 1000), 2) + " KB";
    }

    private static String formatMB(double bytes) {
        return NukkitMath.round((bytes / 1024 / 1024 * 1000), 2) + " MB";
    }

    private static String formatMB(long bytes) {
        return NukkitMath.round((bytes / 1024d / 1024 * 1000), 2) + " MB";
    }

    private static String formatFreq(long hz) {
        if (hz >= 1000000000) {
            return String.format("%.2fGHz", hz / 1000000000.0);
        }
        if (hz >= 1000 * 1000) {
            return String.format("%.2fMHz", hz / 1000000.0);
        }
        if (hz >= 1000) {
            return String.format("%.2fKHz", hz / 1000.0);
        }
        return String.format("%dHz", hz);
    }

    public static String formatUptime(long uptime) {
        long days = TimeUnit.MILLISECONDS.toDays(uptime);
        uptime -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(uptime);
        uptime -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime);
        uptime -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime);
        return String.format(UPTIME_FORMAT, days, hours, minutes, seconds);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String isInVM() {
        // Упрощенная детекция виртуализации без использования OSHI
        String vendor = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : vmVendor.entrySet()) {
            if (vendor.contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        // Docker detection
        var file = new File("/.dockerenv");
        if (file.exists()) {
            return "Docker Container";
        }
        var cgroupFile = new File("/proc/1/cgroup");
        if (cgroupFile.exists()) {
            try (var lineStream = Files.lines(cgroupFile.toPath())) {
                var searchResult = lineStream.filter(line -> line.contains("docker") || line.contains("lxc"));
                if (searchResult.findAny().isPresent()) {
                    return "Docker Container";
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        boolean simpleMode = args.length == 0 || !args[0].equalsIgnoreCase("full");
        Server server = sender.getServer();

        if (simpleMode) {
            sender.sendMessage(TextFormat.GREEN + "---- " + TextFormat.WHITE + "Server status" + TextFormat.GREEN + " ----");

            long time = System.currentTimeMillis() - Nukkit.START_TIME;

            sender.sendMessage(TextFormat.GOLD + "Uptime: " + formatUptime(time));

            TextFormat tpsColor = TextFormat.GREEN;
            float tps = server.getTicksPerSecond();
            if (tps < 12) {
                tpsColor = TextFormat.RED;
            } else if (tps < 17) {
                tpsColor = TextFormat.GOLD;
            }

            sender.sendMessage(TextFormat.GOLD + "Current TPS: " + tpsColor + NukkitMath.round(tps, 2));

            sender.sendMessage(TextFormat.GOLD + "Load: " + tpsColor + server.getTickUsage() + "%");

            sender.sendMessage(TextFormat.GOLD + "Thread count: " + TextFormat.GREEN + Thread.getAllStackTraces().size());

            Runtime runtime = Runtime.getRuntime();
            double totalMB = NukkitMath.round(((double) runtime.totalMemory()) / 1024 / 1024, 2);
            double usedMB = NukkitMath.round((double) (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024, 2);
            double maxMB = NukkitMath.round(((double) runtime.maxMemory()) / 1024 / 1024, 2);
            double usage = usedMB / maxMB * 100;
            TextFormat usageColor = TextFormat.GREEN;

            if (usage > 85) {
                usageColor = TextFormat.GOLD;
            }

            sender.sendMessage(TextFormat.GOLD + "Used VM memory: " + usageColor + usedMB + " MB. (" + NukkitMath.round(usage, 2) + "%)");

            sender.sendMessage(TextFormat.GOLD + "Total VM memory: " + TextFormat.RED + totalMB + " MB.");


            TextFormat playerColor = TextFormat.GREEN;
            if (((float) server.getOnlinePlayers().size() / (float) server.getMaxPlayers()) > 0.85) {
                playerColor = TextFormat.GOLD;
            }

            sender.sendMessage(TextFormat.GOLD + "Players: " + playerColor + server.getOnlinePlayers().size() + TextFormat.GREEN + " online, " +
                    TextFormat.RED + server.getMaxPlayers() + TextFormat.GREEN + " max. ");

            for (Level level : server.getLevels().values()) {
                sender.sendMessage(buildWorldInfo(level));
            }
        } else {
            // 完整模式
            sender.sendMessage(TextFormat.GREEN + "---- " + TextFormat.WHITE + "Server status" + TextFormat.GREEN + " ----");

            // 服务器信息
            {
                sender.sendMessage(TextFormat.YELLOW + ">>> " + TextFormat.RED + "Nukkit" + TextFormat.DARK_AQUA + "-" + TextFormat.LIGHT_PURPLE + "MOT " + TextFormat.RESET + " Server Info" + TextFormat.YELLOW + " <<<" + TextFormat.RESET);
                // 运行时间
                long time = System.currentTimeMillis() - Nukkit.START_TIME;
                sender.sendMessage(TextFormat.GOLD + "Uptime: " + formatUptime(time));
                // TPS
                TextFormat tpsColor = TextFormat.GREEN;
                float tps = server.getTicksPerSecond();
                if (tps < 12) {
                    tpsColor = TextFormat.RED;
                } else if (tps < 17) {
                    tpsColor = TextFormat.GOLD;
                }
                sender.sendMessage(TextFormat.GOLD + "Current TPS: " + tpsColor + NukkitMath.round(tps, 2));
                // 游戏刻负载
                sender.sendMessage(TextFormat.GOLD + "Tick Load: " + tpsColor + server.getTickUsage() + "%");
                // 在线玩家情况
                TextFormat playerColor = TextFormat.GREEN;
                if (((float) server.getOnlinePlayers().size() / (float) server.getMaxPlayers()) > 0.85) {
                    playerColor = TextFormat.GOLD;
                }
                sender.sendMessage(TextFormat.GOLD + "Players: " + playerColor + server.getOnlinePlayers().size() + TextFormat.GREEN + " online, " +
                        TextFormat.RED + server.getMaxPlayers() + TextFormat.GREEN + " max. ");
                // 各个世界的情况
                for (Level level : server.getLevels().values()) {
                    sender.sendMessage(buildWorldInfo(level));
                }
                sender.sendMessage("");
            }
            // 操作系统&JVM 信息
            {
                RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
                sender.sendMessage(TextFormat.YELLOW + ">>> " + TextFormat.WHITE + "OS & JVM Info" + TextFormat.YELLOW + " <<<" + TextFormat.RESET);
                
                String osName = System.getProperty("os.name", "Unknown");
                String osVersion = System.getProperty("os.version", "Unknown");
                String osArch = System.getProperty("os.arch", "Unknown");
                sender.sendMessage(TextFormat.GOLD + "OS: " + TextFormat.AQUA + osName + " " + osVersion + " " + osArch);
                
                sender.sendMessage(TextFormat.GOLD + "JVM: " + TextFormat.AQUA + mxBean.getVmName() + " " + mxBean.getVmVendor() + " " + mxBean.getVmVersion());
                
                try {
                    String vm = isInVM();
                    if (vm == null) {
                        sender.sendMessage(TextFormat.GOLD + "Virtual environment: " + TextFormat.GREEN + "no");
                    } else {
                        sender.sendMessage(TextFormat.GOLD + "Virtual environment: " + TextFormat.YELLOW + "yes (" + vm + ")");
                    }
                } catch (Exception ignore) {
                    sender.sendMessage(TextFormat.GOLD + "Virtual environment: " + TextFormat.GRAY + "unknown");
                }
                sender.sendMessage("");
            }
            // CPU 信息 - с использованием SystemMetrics
            {
                SystemMetrics.CpuStats cpuStats = SystemMetrics.getInstance().getCpuStats();
                sender.sendMessage(TextFormat.YELLOW + ">>> " + TextFormat.WHITE + "CPU Info" + TextFormat.YELLOW + " <<<" + TextFormat.RESET);
                
                String cpuName = System.getProperty("os.arch", "Unknown");
                int cores = Runtime.getRuntime().availableProcessors();
                sender.sendMessage(TextFormat.GOLD + "CPU: " + TextFormat.AQUA + cpuName + TextFormat.GRAY +
                        " (" + cores + " logical cores)");
                sender.sendMessage(TextFormat.GOLD + "Thread count: " + TextFormat.GREEN + Thread.getAllStackTraces().size());
                sender.sendMessage(TextFormat.GOLD + "CPU Usage: " + TextFormat.GREEN + NukkitMath.round(cpuStats.getUsagePercent(), 2) + "%");
                sender.sendMessage("");
            }
            // 内存信息 - с использованием SystemMetrics
            {
                SystemMetrics.MemoryStats memStats = SystemMetrics.getInstance().getMemoryStats();
                long allPhysicalMemory = memStats.totalKb();
                long usedPhysicalMemory = memStats.getUsedKb();
                
                sender.sendMessage(TextFormat.YELLOW + ">>> " + TextFormat.WHITE + "Memory Info" + TextFormat.YELLOW + " <<<" + TextFormat.RESET);
                //JVM 内存
                Runtime runtime = Runtime.getRuntime();
                double totalMB = NukkitMath.round(((double) runtime.totalMemory()) / 1024 / 1024, 2);
                double usedMB = NukkitMath.round((double) (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024, 2);
                double maxMB = NukkitMath.round(((double) runtime.maxMemory()) / 1024 / 1024, 2);
                double usage = usedMB / maxMB * 100;
                TextFormat usageColor = TextFormat.GREEN;
                if (usage > 85) {
                    usageColor = TextFormat.GOLD;
                }
                sender.sendMessage(TextFormat.GOLD + "JVM memory: ");
                sender.sendMessage(TextFormat.GOLD + "  Used JVM memory: " + usageColor + usedMB + " MB. (" + NukkitMath.round(usage, 2) + "%)");
                sender.sendMessage(TextFormat.GOLD + "  Total JVM memory: " + TextFormat.RED + totalMB + " MB.");
                sender.sendMessage(TextFormat.GOLD + "  Maximum JVM memory: " + TextFormat.RED + maxMB + " MB.");
                // 操作系统内存
                if (allPhysicalMemory > 0) {
                    usage = (double) usedPhysicalMemory / allPhysicalMemory * 100;
                    usageColor = TextFormat.GREEN;
                    if (usage > 85) {
                        usageColor = TextFormat.GOLD;
                    }
                    sender.sendMessage(TextFormat.GOLD + "OS memory: ");
                    sender.sendMessage(TextFormat.GOLD + "  Physical memory: " + TextFormat.GREEN + usageColor + formatMB(usedPhysicalMemory) + " / " + formatMB(allPhysicalMemory) + ". (" + NukkitMath.round(usage, 2) + "%)");
                }
                sender.sendMessage("");
            }
        }


        return true;
    }

    private static String buildWorldInfo(Level level) {
        return TextFormat.GOLD + "World \"" + level.getFolderName() + "\""
                + (!Objects.equals(level.getFolderName(), level.getName()) ? " (" + level.getName() + ")" : "") + ": "
                + TextFormat.RED + level.getChunks().size() + TextFormat.GREEN + " chunks, "
                + TextFormat.RED + level.getEntities().length + TextFormat.GREEN + " entities, "
                + TextFormat.RED + level.getBlockEntities().size() + TextFormat.GREEN + " blockEntities."
                + " Time " + ((level.getTickRate() > 1 || level.getTickRateTime() > 40) ? TextFormat.RED : TextFormat.YELLOW)
                + NukkitMath.round(level.getTickRateTime(), 2) + "ms"
                + (level.getTickRate() > 1 ? " (tick rate " + (19 - level.getTickRate()) + ")" : "");
    }

}