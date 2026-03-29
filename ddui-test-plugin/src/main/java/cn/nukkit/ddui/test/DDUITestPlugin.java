package cn.nukkit.ddui.test;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.ddui.CustomForm;
import cn.nukkit.ddui.Observable;
import cn.nukkit.ddui.element.DropdownElement;
import cn.nukkit.ddui.properties.DataDrivenProperty;
import cn.nukkit.plugin.PluginBase;

import java.util.List;

/**
 * DDUI Test Plugin - Tests all DDUI elements in-game.
 * Opens a form with every element type and logs interactions.
 */
public class DDUITestPlugin extends PluginBase {

    private Observable<String> observableText;
    private Observable<Boolean> observableBoolean;
    private Observable<Long> observableLong;
    private Observable<Long> selectedDropdownIndex;

    @Override
    public void onEnable() {
        log("DDUI Test Plugin enabled!");
        log("Available command: /dduitest");

        // Initialize observables
        observableText = new Observable<>("Initial Text");
        observableBoolean = new Observable<>(false);
        observableLong = new Observable<>(50L);
        selectedDropdownIndex = new Observable<>(0L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dduitest")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command is only available for players!");
                return true;
            }

            openTestForm(player);
            return true;
        }
        return false;
    }

    /**
     * Opens the test form containing all DDUI elements.
     */
    private void openTestForm(Player player) {
        log("Opening test form for player: " + player.getName());

        CustomForm form = new CustomForm("§l§6DDUI Element Test");

        // Header
        form.header("§eTesting all DDUI elements");

        // Label
        form.label("§fThis screen tests every DDUI element type.");

        // Spacer
        form.spacer();

        // TextField
        form.textField("§7Enter text:", observableText);

        // Slider
        form.slider("§7Select value (0-100):", 0, 100, observableLong);

        // Toggle
        form.toggle("§7Enable option:", observableBoolean);

        // Dropdown
        List<DropdownElement.Item> dropdownItems = List.of(
            DropdownElement.Item.builder().label("Option 1").description("First option").build(),
            DropdownElement.Item.builder().label("Option 2").description("Second option").build(),
            DropdownElement.Item.builder().label("Option 3").description("Third option").build(),
            DropdownElement.Item.builder().label("Option 4").description("Fourth option").build()
        );
        form.dropdown("§7Select an option:", dropdownItems, selectedDropdownIndex);

        // Spacer
        form.spacer();

        // Buttons - Note: Handlers must return DataDrivenProperty (null if static)
        form.button("§a§lTest Button 1", p -> { onButton1Click(p); return null; });
        form.button("§b§lTest Button 2", p -> { onButton2Click(p); return null; });
        form.button("§c§lTest Button 3", p -> { onButton3Click(p); return null; });

        // Close button
        form.closeButton();

        // Show form
        form.show(player);
        log("Form shown to player: " + player.getName());
    }

    /**
     * Handler for Button 1.
     */
    private void onButton1Click(Player player) {
        log("=== BUTTON 1 CLICKED ===");
        log("Player: " + player.getName());
        log("TextField value: " + observableText.getValue());
        log("Toggle value: " + observableBoolean.getValue());
        log("Slider value: " + observableLong.getValue());
        log("Dropdown index: " + selectedDropdownIndex.getValue());
        log("========================");

        player.sendMessage("§a[DDUI Test] Button 1 clicked! Check console for logs.");

        // Update values to demonstrate reactivity
        observableText.setValue("Changed after click!");
        observableBoolean.setValue(!observableBoolean.getValue());
    }

    /**
     * Handler for Button 2.
     */
    private void onButton2Click(Player player) {
        log("=== BUTTON 2 CLICKED ===");
        log("Player: " + player.getName());
        log("All current values logged.");
        log("========================");

        player.sendMessage("§b[DDUI Test] Button 2 clicked! Values reset.");

        // Reset values
        observableText.setValue("New Text");
        observableBoolean.setValue(false);
        observableLong.setValue(50L);
        selectedDropdownIndex.setValue(0L);
    }

    /**
     * Handler for Button 3.
     */
    private void onButton3Click(Player player) {
        log("=== BUTTON 3 CLICKED ===");
        log("Player: " + player.getName());
        log("Testing dynamic element updates.");
        log("========================");

        player.sendMessage("§c[DDUI Test] Button 3 clicked! Elements updated.");

        // Demonstrate dynamic updates
        observableText.setValue("Dynamically changed!");
        observableLong.setValue(observableLong.getValue() + 10);
    }

    /**
     * Logs a message to the console with the plugin prefix.
     */
    private void log(String message) {
        Server.getInstance().getLogger().info("[DDUI-Test] " + message);
    }
}
