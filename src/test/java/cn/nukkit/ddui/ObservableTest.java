package cn.nukkit.ddui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for class Observable
 */
public class ObservableTest {

    @Test
    public void testConstructor() {
        Observable<String> obs = new Observable<>("initial");
        assertEquals("initial", obs.getValue());
    }

    @Test
    public void testSetValue() {
        Observable<String> obs = new Observable<>("initial");
        obs.setValue("new value");
        assertEquals("new value", obs.getValue());
    }

    @Test
    public void testSubscribeAndNotify() {
        Observable<String> obs = new Observable<>("initial");
        final boolean[] notified = {false};
        final String[] receivedValue = {null};

        obs.subscribe(value -> {
            notified[0] = true;
            receivedValue[0] = value;
            return null;
        });

        obs.setValue("updated");
        assertTrue(notified[0]);
        assertEquals("updated", receivedValue[0]);
    }

    @Test
    public void testUnsubscribe() {
        Observable<String> obs = new Observable<>("initial");
        final int[] callCount = {0};

        Observable.Listener<String> listener = value -> {
            callCount[0]++;
            return null;
        };

        obs.subscribe(listener);
        obs.setValue("first");
        assertEquals(1, callCount[0]);

        obs.unsubscribe(listener);
        obs.setValue("second");
        assertEquals(1, callCount[0]);
    }

    @Test
    public void testMultipleSubscribers() {
        Observable<Integer> obs = new Observable<>(0);
        final int[] count1 = {0};
        final int[] count2 = {0};

        obs.subscribe(v -> { count1[0] = v; return null; });
        obs.subscribe(v -> { count2[0] = v * 2; return null; });

        obs.setValue(5);
        assertEquals(5, count1[0]);
        assertEquals(10, count2[0]);
    }

    @Test
    public void testNullValue() {
        Observable<String> obs = new Observable<>(null);
        assertNull(obs.getValue());
        obs.setValue(null);
        assertNull(obs.getValue());
    }

    @Test
    public void testWithOutboundSuppressed() {
        Observable<String> obs = new Observable<>("test");
        final boolean[] called = {false};

        obs.subscribe(value -> {
            called[0] = true;
            return null;
        });

        Observable.withOutboundSuppressed(() -> {
            obs.setValue("suppressed");
        });

        assertTrue(called[0]);
    }
}
