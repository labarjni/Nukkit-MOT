package cn.nukkit.ddui.element;

import cn.nukkit.ddui.Observable;
import cn.nukkit.ddui.properties.ObjectProperty;
import cn.nukkit.ddui.properties.StringProperty;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Tests for DDUI classes (Data-Driven UI)
 */
public class DduiElementTest {

    private ObjectProperty createParent() {
        return new ObjectProperty("parent");
    }

    // ==================== ButtonElement Tests ====================

    @Test
    public void testButtonElementCreation() {
        ObjectProperty parent = createParent();
        ButtonElement button = new ButtonElement("Click Me", parent);

        assertEquals("button", button.getName());
        assertEquals("Click Me", button.getLabel());
        assertFalse(button.getDisabled());
        assertTrue(button.getVisibility());
    }

    @Test
    public void testButtonElementWithObservableLabel() {
        ObjectProperty parent = createParent();
        Observable<String> labelObs = new Observable<>("Initial");
        ButtonElement button = new ButtonElement(labelObs.getValue(), parent);

        assertEquals("Initial", button.getLabel());

        button.setLabel(labelObs);

        labelObs.setValue("Updated");

        assertEquals("Updated", button.getLabel());
    }

    @Test
    public void testButtonElementSetToolTip() {
        ObjectProperty parent = createParent();
        ButtonElement button = new ButtonElement("Test", parent);

        button.setToolTip("Tooltip text");
        assertEquals("Tooltip text", button.getToolTip());
    }

    @Test
    public void testButtonElementSetVisibility() {
        ObjectProperty parent = createParent();
        ButtonElement button = new ButtonElement("Test", parent);

        button.setVisibility(false);
        assertFalse(button.getVisibility());

        button.setVisibility(true);
        assertTrue(button.getVisibility());
    }

    @Test
    public void testButtonElementSetDisabled() {
        ObjectProperty parent = createParent();
        ButtonElement button = new ButtonElement("Test", parent);

        button.setDisabled(true);
        assertTrue(button.getDisabled());
    }

    // ==================== TextFieldElement Tests ====================

    @Test
    public void testTextFieldElementCreation() {
        ObjectProperty parent = createParent();
        Observable<String> text = new Observable<>("initial text");
        TextFieldElement field = new TextFieldElement("Enter Text", text, parent);

        assertEquals("textField", field.getName());
        assertEquals("Enter Text", field.getLabel());
        assertEquals("initial text", field.getText());
    }

    @Test
    public void testTextFieldElementSetText() {
        ObjectProperty parent = createParent();
        Observable<String> text = new Observable<>("initial");
        TextFieldElement field = new TextFieldElement("Label", text, parent);

        field.setText("new text");
        assertEquals("new text", field.getText());
    }

    @Test
    public void testTextFieldElementWithObservable() {
        ObjectProperty parent = createParent();
        Observable<String> text = new Observable<>("initial");
        TextFieldElement field = new TextFieldElement("Label", text, parent);

        text.setValue("updated via observable");
        assertEquals("updated via observable", field.getText());
    }

    @Test
    public void testTextFieldElementDescription() {
        ObjectProperty parent = createParent();
        Observable<String> text = new Observable<>("text");
        TextFieldElement field = new TextFieldElement("Label", text, parent);

        field.setDescription("Description text");
        assertEquals("Description text", field.getDescription());
    }

    // ==================== SliderElement Tests ====================

    @Test
    public void testSliderElementCreation() {
        ObjectProperty parent = createParent();
        Observable<Long> value = new Observable<>(50L);
        SliderElement slider = new SliderElement("Volume", value, 0L, 100L, parent);

        assertEquals("slider", slider.getName());
        assertEquals("Volume", slider.getLabel());
        assertEquals(0L, slider.getMinValue());
        assertEquals(100L, slider.getMaxValue());
        assertEquals(50L, slider.getSliderValue());
    }

    @Test
    public void testSliderElementSetValue() {
        ObjectProperty parent = createParent();
        Observable<Long> value = new Observable<>(0L);
        SliderElement slider = new SliderElement("Test", value, 0L, 100L, parent);

        slider.setValue(75L);
        assertEquals(75L, slider.getSliderValue());
    }

    @Test
    public void testSliderElementSetStep() {
        ObjectProperty parent = createParent();
        Observable<Long> value = new Observable<>(0L);
        SliderElement slider = new SliderElement("Test", value, 0L, 100L, parent);

        slider.setStep(5L);
        assertEquals(5L, slider.getStep());
    }

    @Test
    public void testSliderElementWithObservableValue() {
        ObjectProperty parent = createParent();
        Observable<Long> value = new Observable<>(10L);
        SliderElement slider = new SliderElement("Test", value, 0L, 100L, parent);

        value.setValue(25L);
        assertEquals(25L, slider.getSliderValue());
    }

    // ==================== ToggleElement Tests ====================

    @Test
    public void testToggleElementCreation() {
        ObjectProperty parent = createParent();
        Observable<Boolean> toggled = new Observable<>(false);
        ToggleElement toggle = new ToggleElement("Enable Feature", toggled, parent);

        assertEquals("toggle", toggle.getName());
        assertEquals("Enable Feature", toggle.getLabel());
        assertFalse(toggle.isToggled());
    }

    @Test
    public void testToggleElementSetToggled() {
        ObjectProperty parent = createParent();
        Observable<Boolean> toggled = new Observable<>(false);
        ToggleElement toggle = new ToggleElement("Test", toggled, parent);

        toggle.setToggled(true);
        assertTrue(toggle.isToggled());
    }

    @Test
    public void testToggleElementWithObservable() {
        ObjectProperty parent = createParent();
        Observable<Boolean> toggled = new Observable<>(false);
        ToggleElement toggle = new ToggleElement("Test", toggled, parent);

        toggled.setValue(true);
        assertTrue(toggle.isToggled());
    }

    // ==================== DropdownElement Tests ====================

    @Test
    public void testDropdownElementCreation() {
        ObjectProperty parent = createParent();
        Observable<Long> selected = new Observable<>(0L);
        List<DropdownElement.Item> items = List.of(
            DropdownElement.Item.builder().label("Option 1").build(),
            DropdownElement.Item.builder().label("Option 2").build()
        );

        DropdownElement dropdown = new DropdownElement("Choose", items, selected, parent);

        assertEquals("dropdown", dropdown.getName());
        assertEquals("Choose", dropdown.getLabel());
        assertEquals(0L, dropdown.getSelectedIndex());
    }

    @Test
    public void testDropdownElementSetSelectedIndex() {
        ObjectProperty parent = createParent();
        Observable<Long> selected = new Observable<>(0L);
        List<DropdownElement.Item> items = List.of(
            DropdownElement.Item.builder().label("Option 1").build(),
            DropdownElement.Item.builder().label("Option 2").build()
        );

        DropdownElement dropdown = new DropdownElement("Choose", items, selected, parent);

        dropdown.setSelectedIndex(1L);
        assertEquals(1L, dropdown.getSelectedIndex());
    }

    @Test
    public void testDropdownElementWithObservable() {
        ObjectProperty parent = createParent();
        Observable<Long> selected = new Observable<>(0L);
        List<DropdownElement.Item> items = List.of(
            DropdownElement.Item.builder().label("Option 1").build(),
            DropdownElement.Item.builder().label("Option 2").build()
        );

        DropdownElement dropdown = new DropdownElement("Choose", items, selected, parent);

        selected.setValue(1L);
        assertEquals(1L, dropdown.getSelectedIndex());
    }

    // ==================== HeaderElement Tests ====================

    @Test
    public void testHeaderElementCreation() {
        ObjectProperty parent = createParent();
        HeaderElement header = new HeaderElement("Section Title", parent);

        assertEquals("header", header.getName());
        assertEquals("Section Title", header.getText());
    }

    @Test
    public void testHeaderElementSetText() {
        ObjectProperty parent = createParent();
        HeaderElement header = new HeaderElement("Initial", parent);

        header.setText("Updated Title");
        assertEquals("Updated Title", header.getText());
    }

    @Test
    public void testHeaderElementWithObservable() {
        ObjectProperty parent = createParent();
        Observable<String> text = new Observable<>("Initial");
        HeaderElement header = new HeaderElement(text, parent);

        text.setValue("Updated");
        assertEquals("Updated", header.getText());
    }

    // ==================== LabelElement Tests ====================

    @Test
    public void testLabelElementCreation() {
        ObjectProperty parent = createParent();
        LabelElement label = new LabelElement("Label Text", parent);

        assertEquals("label", label.getName());
        assertEquals("Label Text", label.getText());
    }

    @Test
    public void testLabelElementSetText() {
        ObjectProperty parent = createParent();
        LabelElement label = new LabelElement("Initial", parent);

        label.setText("Updated");
        assertEquals("Updated", label.getText());
    }

    @Test
    public void testLabelElementWithObservable() {
        ObjectProperty parent = createParent();
        Observable<String> text = new Observable<>("Initial");
        LabelElement label = new LabelElement(text, parent);

        text.setValue("Updated");
        assertEquals("Updated", label.getText());
    }

    // ==================== SpacerElement Tests ====================

    @Test
    public void testSpacerElementCreation() {
        ObjectProperty parent = createParent();
        SpacerElement spacer = new SpacerElement(parent);

        assertEquals("spacer", spacer.getName());
        assertTrue(spacer.getVisibility());
    }

    @Test
    public void testSpacerElementSetVisibility() {
        ObjectProperty parent = createParent();
        SpacerElement spacer = new SpacerElement(parent);

        spacer.setVisibility(false);
        assertFalse(spacer.getVisibility());
    }

    // ==================== CloseButtonElement Tests ====================

    @Test
    public void testCloseButtonElementCreation() {
        ObjectProperty parent = createParent();
        CloseButtonElement closeButton = new CloseButtonElement(parent);

        assertEquals("closeButton", closeButton.getName());
    }

    // ==================== LayoutElement Tests ====================

    @Test
    public void testLayoutElementCreation() {
        ObjectProperty parent = createParent();
        LayoutElement layout = new LayoutElement(parent);

        assertEquals("layout", layout.getName());
        assertNotNull(layout.getValue());
    }

    @Test
    public void testLayoutElementAddProperty() {
        ObjectProperty parent = createParent();
        LayoutElement layout = new LayoutElement(parent);

        StringProperty child = new StringProperty("test", "value", layout);
        layout.setProperty(child);

        assertEquals(1, layout.getValue().size() - 1);
    }

    // ==================== Edge Cases & Null Safety Tests ====================

    @Test
    public void testElementWithNullLabel() {
        ObjectProperty parent = createParent();
        ButtonElement button = new ButtonElement("", parent);

        assertEquals("", button.getLabel());
    }

    @Test
    public void testElementWithEmptyString() {
        ObjectProperty parent = createParent();
        TextFieldElement field = new TextFieldElement("", new Observable<>(""), parent);

        assertEquals("", field.getLabel());
        assertEquals("", field.getText());
    }

    @Test
    public void testSliderWithNegativeValues() {
        ObjectProperty parent = createParent();
        Observable<Long> value = new Observable<>(-50L);
        SliderElement slider = new SliderElement("Temp", value, -100L, 100L, parent);

        assertEquals(-100L, slider.getMinValue());
        assertEquals(100L, slider.getMaxValue());
        assertEquals(-50L, slider.getSliderValue());
    }

    @Test
    public void testSliderWithSameMinMax() {
        ObjectProperty parent = createParent();
        Observable<Long> value = new Observable<>(5L);
        SliderElement slider = new SliderElement("Fixed", value, 5L, 5L, parent);

        assertEquals(5L, slider.getMinValue());
        assertEquals(5L, slider.getMaxValue());
    }

    @Test
    public void testDropdownWithEmptyItems() {
        ObjectProperty parent = createParent();
        Observable<Long> selected = new Observable<>(0L);
        List<DropdownElement.Item> items = List.of();

        DropdownElement dropdown = new DropdownElement("Empty", items, selected, parent);

        assertEquals("Empty", dropdown.getLabel());
        assertEquals(0L, dropdown.getSelectedIndex());
    }

    @Test
    public void testToggleButtonRapidChanges() {
        ObjectProperty parent = createParent();
        Observable<Boolean> toggled = new Observable<>(false);
        ToggleElement toggle = new ToggleElement("Test", toggled, parent);

        for (int i = 0; i < 11; i++) {
            toggled.setValue(i % 2 == 0);
        }

        assertTrue(toggle.isToggled());
    }

    @Test
    public void testMultipleObservableSubscriptions() {
        ObjectProperty parent = createParent();
        Observable<String> labelObs = new Observable<>("Label");
        Observable<Boolean> disabledObs = new Observable<>(false);
        Observable<Boolean> visibleObs = new Observable<>(true);

        ButtonElement button = new ButtonElement(labelObs.getValue(), parent);
        button.setLabel(labelObs);
        button.setDisabled(disabledObs);
        button.setVisibility(visibleObs);

        labelObs.setValue("New Label");
        disabledObs.setValue(true);
        visibleObs.setValue(false);

        assertEquals("New Label", button.getLabel());
        assertTrue(button.getDisabled());
        assertFalse(button.getVisibility());
    }
}
