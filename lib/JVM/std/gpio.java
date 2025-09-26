package std;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalState; // <-- THIS IS REQUIRED


import java.util.HashMap;
import java.util.Map;

public class gpio {

    private static Context pi4j = Pi4J.newAutoContext();

    // Store created DigitalOutput instances by pin number
    private static Map<Integer, DigitalOutput> ledMap = new HashMap<>();

    private static DigitalOutput getLedOutput(int pinNumber) {
        if (ledMap.containsKey(pinNumber)) {
            return ledMap.get(pinNumber);
        }
    
    DigitalOutputConfigBuilder config = DigitalOutput.newConfigBuilder(pi4j)
        .id("led-" + pinNumber)
        .name("LED Pin " + pinNumber)
        .address(pinNumber)
        .shutdown(DigitalState.LOW)
        .initial(DigitalState.LOW)
        .provider("gpiod-digital-output");

        DigitalOutput led = pi4j.create(config.build());

        ledMap.put(pinNumber, led);  // Save it for reuse
        return led;
    }

    public static void turnOnLed(int pinNumber) {
        DigitalOutput led = getLedOutput(pinNumber);
        System.out.println("LED high - pin NO." + pinNumber);
        led.high();
    }

    public static void turnOffLed(int pinNumber) {
        DigitalOutput led = getLedOutput(pinNumber);
        System.out.println("LED low -pin NO." + pinNumber);
        led.low();
    }

    public static void shutdown() {
        pi4j.shutdown();
    }
}
