import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.spi.*;
import java.nio.ByteBuffer;

public class MAX31855Reader {

    //private final Context pi4j;
    //private final Spi spi;

        /** Map a Raspberry Pi GPIO pin to SPI0 chip-select. */
    private static SpiChipSelect chipSelectFromGpio(int gpioPin) {
        switch (gpioPin) {
            case 8:  return SpiChipSelect.CS_0;   // CE0
            case 7:  return SpiChipSelect.CS_1;   // CE1
            default:
                throw new IllegalArgumentException(
                        "Unsupported GPIO pin: " + gpioPin + 
                        ". Use GPIO8 (CE0) or GPIO7 (CE1)."
                );
        }
    }

    /** Read temperature (°C) from MAX31855 using the given chip-select GPIO pin. */
    public static double read_temp(int gpio_pin) throws Exception {
        Context ctx = null;
        Spi spiLocal = null;

        try {
            SpiChipSelect cs = chipSelectFromGpio(gpio_pin);

            ctx = Pi4J.newAutoContext();
            SpiConfig cfg = Spi.newConfigBuilder(ctx)
                    .id("spi0-" + cs)
                    .name("SPI0 " + cs)
                    .bus(SpiBus.BUS_0)
                    .chipSelect(cs)
                    .baud(5_000_000)
                    .mode(SpiMode.MODE_0)
                    .build();

            spiLocal = ctx.create(cfg);

            // Read 32-bit frame
            byte[] rx = new byte[4];
            spiLocal.read(rx);
            int raw = java.nio.ByteBuffer.wrap(rx).getInt();

            // Fault check
            boolean fault = ((raw & 0x00010000) != 0);
            if (fault) {
                throw new IllegalStateException(
                        "MAX31855 fault detected (SCV/SCG/OC). Raw=0x" + 
                        Integer.toHexString(raw).toUpperCase()
                );
            }

            // Decode thermocouple temperature (bits 31..18)
            int ext14 = (raw >> 18) & 0x3FFF;
            if ((ext14 & 0x2000) != 0) ext14 |= ~0x3FFF; // sign extend
            return ext14 * 0.25;

        } finally {
            try { if (spiLocal != null) spiLocal.close(); } catch (Exception ignore) {}
            if (ctx != null) ctx.shutdown();
        }
    }



    public static void main(String[] args) throws Exception {
        double t = read_temp(8);   // GPIO8 = CE0
        System.out.println("Temperature = " + t + " °C");
    }

}