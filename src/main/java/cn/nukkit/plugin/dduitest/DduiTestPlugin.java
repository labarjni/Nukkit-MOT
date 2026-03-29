package cn.nukkit.plugin.dduitest;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.ddui.CustomForm;
import cn.nukkit.ddui.Observable;
import cn.nukkit.ddui.element.DropdownElement;
import cn.nukkit.ddui.element.options.*;
import cn.nukkit.plugin.PluginBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * DDUI Test Plugin - 用于测试所有 Data-Driven UI 元素
 * 在服务器中运行后，使用 /dduitest 命令打开测试界面
 */
public class DduiTestPlugin extends PluginBase {
    
    private static final Logger LOG = LogManager.getLogger("DduiTest");
    
    // Observable values for testing reactive updates
    private final Observable<String> titleText = new Observable<>("DDUI 元素测试面板");
    private final Observable<String> labelText = new Observable<>("Label 初始文本");
    private final Observable<String> headerText = new Observable<>("Header 标题测试");
    private final Observable<Boolean> toggleValue = new Observable<>(false);
    private final Observable<Long> sliderValue = new Observable<>(50L);
    private final Observable<String> textFieldValue = new Observable<>("");
    private final Observable<Long> dropdownSelection = new Observable<>(0L);
    private final Observable<Boolean> buttonVisible = new Observable<>(true);
    private final Observable<Boolean> labelVisible = new Observable<>(true);
    private final Observable<Boolean> toggleVisible = new Observable<>(true);
    private final Observable<Boolean> sliderVisible = new Observable<>(true);
    private final Observable<Boolean> textVisible = new Observable<>(true);
    private final Observable<Boolean> dropdownVisible = new Observable<>(true);
    private final Observable<Boolean> spacerVisible = new Observable<>(true);
    
    @Override
    public void onEnable() {
        LOG.info("===========================================");
        LOG.info("DDUI Test Plugin Enabled!");
        LOG.info("Use /dduitest command to open test screen");
        LOG.info("===========================================");
    }
    
    @Override
    public void onDisable() {
        LOG.info("DDUI Test Plugin Disabled!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dduitest")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("请在游戏中使用此命令！");
                return true;
            }
            
            showTestForm(player);
            return true;
        }
        return false;
    }
    
    private void showTestForm(Player player) {
        LOG.info("[DDUI] Opening test form for player: {}", player.getName());
        
        CustomForm form = new CustomForm(titleText);
        
        // ===== HEADER ELEMENT TEST =====
        LOG.info("[DDUI] Adding HeaderElement");
        form.header(headerText, HeaderOptions.builder().visible(true).build());
        headerText.subscribe(val -> 
            LOG.info("[DDUI] Header text changed to: {}", val)
        );
        
        // ===== LABEL ELEMENT TEST =====
        LOG.info("[DDUI] Adding LabelElement");
        form.label(labelText, LabelOptions.builder().visible(labelVisible).build());
        labelText.subscribe(val -> 
            LOG.info("[DDUI] Label text changed to: {}", val)
        );
        
        // ===== SPACER ELEMENT TEST =====
        LOG.info("[DDUI] Adding SpacerElement");
        form.spacer(SpacerOptions.builder().visible(spacerVisible).build());
        
        // ===== BUTTON ELEMENT TEST =====
        LOG.info("[DDUI] Adding ButtonElement");
        form.button("点击我测试按钮", p -> {
            LOG.info("[DDUI] >>> BUTTON CLICKED by: {}", p.getName());
            p.sendMessage("按钮被点击了！查看控制台日志");
        }, ButtonOptions.builder()
                .tooltip("这是一个测试按钮")
                .visible(buttonVisible)
                .disabled(false)
                .build());
        buttonVisible.subscribe(val -> 
            LOG.info("[DDUI] Button visibility changed to: {}", val)
        );
        
        // ===== TOGGLE ELEMENT TEST =====
        LOG.info("[DDUI] Adding ToggleElement");
        form.toggle("开关测试 (Toggle)", toggleValue, ToggleOptions.builder()
                .description("这是一个开关元素的描述")
                .visible(toggleVisible)
                .build());
        toggleValue.subscribe(val -> 
            LOG.info("[DDUI] >>> TOGGLE CHANGED to: {}", val)
        );
        
        // ===== SLIDER ELEMENT TEST =====
        LOG.info("[DDUI] Adding SliderElement");
        form.slider("滑块测试 (Slider)", 0L, 100L, sliderValue, SliderElementOptions.builder()
                .description("拖动滑块改变数值")
                .step(5L)
                .visible(sliderVisible)
                .build());
        sliderValue.subscribe(val -> 
            LOG.info("[DDUI] >>> SLIDER CHANGED to: {}", val)
        );
        
        // ===== TEXTFIELD ELEMENT TEST =====
        LOG.info("[DDUI] Adding TextFieldElement");
        form.textField("文本框测试 (TextField)", textFieldValue, TextFieldOptions.builder()
                .description("在文本框中输入内容")
                .visible(textVisible)
                .build());
        textFieldValue.subscribe(val -> 
            LOG.info("[DDUI] >>> TEXTFIELD CHANGED to: {}", val)
        );
        
        // ===== DROPDOWN ELEMENT TEST =====
        LOG.info("[DDUI] Adding DropdownElement");
        List<DropdownElement.Item> items = List.of(
            DropdownElement.Item.builder().label("选项一").description("第一个选项").build(),
            DropdownElement.Item.builder().label("选项二").description("第二个选项").build(),
            DropdownElement.Item.builder().label("选项三").description("第三个选项").build()
        );
        form.dropdown("下拉菜单测试 (Dropdown)", items, dropdownSelection, DropdownOptions.builder()
                .description("选择一个选项")
                .visible(dropdownVisible)
                .build());
        dropdownSelection.subscribe(val -> 
            LOG.info("[DDUI] >>> DROPDOWN SELECTION CHANGED to index: {}", val)
        );
        
        // ===== CLOSE BUTTON TEST =====
        LOG.info("[DDUI] Adding CloseButtonElement");
        form.closeButton(CloseButtonOptions.builder()
                .label("关闭")
                .visible(true)
                .build());
        
        // Show the form
        form.show(player);
        LOG.info("[DDUI] Form shown to player: {}", player.getName());
        
        // Log all viewers
        form.getAllViewers().forEach(v -> 
            LOG.info("[DDUI] Current viewer: {}", v.getName())
        );
    }
}
