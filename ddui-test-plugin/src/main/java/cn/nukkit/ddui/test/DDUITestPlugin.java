package cn.nukkit.ddui.test;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.ddui.CustomForm;
import cn.nukkit.ddui.Observable;
import cn.nukkit.ddui.element.DropdownElement;
import cn.nukkit.plugin.PluginBase;

import java.util.List;

/**
 * DDUI Test Plugin - плагин для тестирования всех элементов DDUI в игре
 * Открывает форму с каждым типом элемента и логирует все взаимодействия
 */
public class DDUITestPlugin extends PluginBase {

    private Observable<String> observableText;
    private Observable<Boolean> observableBoolean;
    private Observable<Long> observableLong;
    private Observable<Long> selectedDropdownIndex;

    @Override
    public void onEnable() {
        log("DDUI Test Plugin включен!");
        log("Доступные команды: /dduitest");
        
        // Инициализация наблюдаемых переменных
        observableText = new Observable<>("Initial Text");
        observableBoolean = new Observable<>(false);
        observableLong = new Observable<>(50L);
        selectedDropdownIndex = new Observable<>(0L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("dduitest")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Эта команда доступна только игрокам!");
                return true;
            }
            
            openTestForm(player);
            return true;
        }
        return false;
    }

    /**
     * Открывает тестовую форму со всеми элементами DDUI
     */
    private void openTestForm(Player player) {
        log("Открытие тестовой формы для игрока: " + player.getName());
        
        CustomForm form = new CustomForm("§l§6DDUI Element Test");
        
        // Header Element - заголовок
        log("Добавление HeaderElement");
        form.header("§eТест всех элементов DDUI");
        
        // Label Element - текстовая метка
        log("Добавление LabelElement");
        form.label("§fЭтот экран тестирует каждый тип элемента DDUI");
        
        // Spacer Element - разделитель
        log("Добавление SpacerElement");
        form.spacer();
        
        // TextField Element - текстовое поле
        log("Добавление TextFieldElement");
        form.textField("§7Введите текст:", observableText);
        
        // Slider Element - ползунок
        log("Добавление SliderElement");
        form.slider("§7Выберите значение (0-100):", 0, 100, observableLong);
        
        // Toggle Element - переключатель
        log("Добавление ToggleElement");
        form.toggle("§7Включить опцию:", observableBoolean);
        
        // Dropdown Element - выпадающий список
        log("Добавление DropdownElement");
        List<DropdownElement.Item> dropdownItems = List.of(
            DropdownElement.Item.builder().label("Опция 1").description("Первая опция").build(),
            DropdownElement.Item.builder().label("Опция 2").description("Вторая опция").build(),
            DropdownElement.Item.builder().label("Опция 3").description("Третья опция").build(),
            DropdownElement.Item.builder().label("Опция 4").description("Четвертая опция").build()
        );
        form.dropdown("§7Выберите опцию:", dropdownItems, selectedDropdownIndex);
        
        // Spacer Element - разделитель
        form.spacer();
        
        // Button Elements - кнопки для тестирования
        log("Добавление ButtonElement");
        form.button("§a§lТест кнопки 1", this::onButton1Click);
        form.button("§b§lТест кнопки 2", this::onButton2Click);
        form.button("§c§lТест кнопки 3", this::onButton3Click);
        
        // Кнопка закрытия
        form.closeButton();
        
        // Показываем форму игроку
        form.show(player);
        log("Форма показана игроку: " + player.getName());
    }

    /**
     * Обработчик нажатия на кнопку 1
     */
    private void onButton1Click(Player player) {
        log("=== BUTTON 1 CLICKED ===");
        log("Игрок: " + player.getName());
        log("Текст из TextField: " + observableText.getValue());
        log("Значение Toggle: " + observableBoolean.getValue());
        log("Значение Slider: " + observableLong.getValue());
        log("Выбранный индекс Dropdown: " + selectedDropdownIndex.getValue());
        log("========================");
        
        player.sendMessage("§a[DDUI Test] Кнопка 1 нажата! Проверьте консоль для логов.");
        
        // Обновляем значения для демонстрации реактивности
        observableText.setValue("Изменено после клика!");
        observableBoolean.setValue(!observableBoolean.getValue());
    }

    /**
     * Обработчик нажатия на кнопку 2
     */
    private void onButton2Click(Player player) {
        log("=== BUTTON 2 CLICKED ===");
        log("Игрок: " + player.getName());
        log("Все текущие значения были залогированы");
        log("========================");
        
        player.sendMessage("§b[DDUI Test] Кнопка 2 нажата! Значения сброшены.");
        
        // Сбрасываем значения
        observableText.setValue("Новый текст");
        observableBoolean.setValue(false);
        observableLong.setValue(50L);
        selectedDropdownIndex.setValue(0L);
    }

    /**
     * Обработчик нажатия на кнопку 3
     */
    private void onButton3Click(Player player) {
        log("=== BUTTON 3 CLICKED ===");
        log("Игрок: " + player.getName());
        log("Тест динамического обновления элементов");
        log("========================");
        
        player.sendMessage("§c[DDUI Test] Кнопка 3 нажата! Элементы обновлены.");
        
        // Демонстрация динамического обновления
        observableText.setValue("Динамически изменено!");
        observableLong.setValue(observableLong.getValue() + 10);
    }

    /**
     * Логирование в консоль с префиксом плагина
     */
    private void log(String message) {
        Server.getInstance().getLogger().info("[DDUI-Test] " + message);
    }
}
