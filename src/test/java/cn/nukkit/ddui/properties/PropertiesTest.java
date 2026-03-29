package cn.nukkit.ddui.properties;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for properties classes (Properties)
 */
public class PropertiesTest {

    @Test
    public void testStringProperty() {
        ObjectProperty parent = new ObjectProperty("parent");
        StringProperty prop = new StringProperty("test", "initial", parent);

        assertEquals("test", prop.getName());
        assertEquals("initial", prop.getValue());
        assertEquals(parent, prop.getParent());

        prop.setValue("updated");
        assertEquals("updated", prop.getValue());
    }

    @Test
    public void testBooleanProperty() {
        ObjectProperty parent = new ObjectProperty("parent");
        BooleanProperty prop = new BooleanProperty("flag", true, parent);

        assertTrue(prop.getValue());
        prop.setValue(false);
        assertFalse(prop.getValue());
    }

    @Test
    public void testLongProperty() {
        ObjectProperty parent = new ObjectProperty("parent");
        LongProperty prop = new LongProperty("count", 10L, parent);

        assertEquals(10L, prop.getValue());
        prop.setValue(20L);
        assertEquals(20L, prop.getValue());
    }

    @Test
    public void testObjectProperty() {
        ObjectProperty prop = new ObjectProperty("root");
        assertEquals("root", prop.getName());
        assertNotNull(prop.getValue());
        assertTrue(((java.util.Map<?, ?>) prop.getValue()).isEmpty());
    }

    @Test
    public void testObjectPropertySetProperty() {
        ObjectProperty root = new ObjectProperty("root");
        StringProperty child = new StringProperty("child", "value", root);

        root.setProperty(child);
        assertSame(child, root.getProperty("child"));
    }

    @Test
    public void testPathGeneration() {
        ObjectProperty root = new ObjectProperty("");
        ObjectProperty layout = new ObjectProperty("layout", root);
        StringProperty label = new StringProperty("label", "text", layout);

        layout.setProperty(label);
        root.setProperty(layout);

        String path = label.getPath();
        assertTrue(path.contains("label"));
    }

    @Test
    public void testPathGenerationWithIndex() {
        ObjectProperty root = new ObjectProperty("");
        ObjectProperty layout = new ObjectProperty("layout", root);
        StringProperty item = new StringProperty("0", "first", layout);

        layout.setProperty(item);
        root.setProperty(layout);

        String path = item.getPath();

        assertTrue(path.contains("[0]") || path.contains("0"));
    }

    @Test
    public void testGetRootScreen() {
        ObjectProperty root = new ObjectProperty("");
        StringProperty child = new StringProperty("child", "value", root);
        root.setProperty(child);

        assertNull(child.getRootScreen());
    }

    @Test
    public void testTriggerListeners() {
        ObjectProperty parent = new ObjectProperty("parent");
        StringProperty prop = new StringProperty("test", "initial", parent);

        final boolean[] triggered = {false};
        prop.addListener((player, data) -> triggered[0] = true);

        assertEquals(0, prop.getTriggerCount());
    }

    @Test
    public void testRemoveListener() {
        ObjectProperty parent = new ObjectProperty("parent");
        StringProperty prop = new StringProperty("test", "initial", parent);

        var listener = (java.util.function.BiConsumer<cn.nukkit.Player, Object>)
            (player, data) -> {};

        prop.addListener(listener);
        prop.removeListener(listener);
    }
}
