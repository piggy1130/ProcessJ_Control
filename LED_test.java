import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder;
import com.pi4j.io.gpio.digital.DigitalState; // <-- THIS IS REQUIRED

public class LED_test {
    public static void main(String[] args) throws Exception {
        Context pi4j = Pi4J.newAutoContext();

        DigitalOutputConfigBuilder config = DigitalOutput.newConfigBuilder(pi4j)
            .id("gpio21")
            .name("LED")
            .address(21)
            .shutdown(DigitalState.LOW)     // <-- FIXED
            .initial(DigitalState.LOW);     // <-- FIXED

        DigitalOutput output = pi4j.create(config);

        System.out.println("Turning GPIO 21 ON ... ");
        output.high();

        Thread.sleep(5000);


        // System.out.println("Turning GPIO 21 OFF ... ");
        // output.low();

        // pi4j.shutdown();
    }
}
