package std;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.spi.*;
import com.pi4j.io.gpio.digital.*;

import java.nio.ByteBuffer;

public class jlMAX31855reader {
    
    public static void jl_print_2(){
        System.out.println("Jiaqi is testing....MAX31855");
    }

    /** Hardware SPI0 using hardware chip-select (CE0/CE1). */
    // if gpio pin = 8 -> CS0
    // if gpio pin = 7 -> CS1
    public static double readTemp_HwCS(int gpioPin) {

        SpiChipSelect cs;
        if (gpioPin == 8) {
            cs = SpiChipSelect.CS_0;   // CE0
        } else if (gpioPin == 7) {
            cs = SpiChipSelect.CS_1;   // CE1
        } else {
            throw new IllegalArgumentException("Only GPIO7 (CE1) or GPIO8 (CE0) are valid hardware CS pins.");
        }

        Context ctx = Pi4J.newAutoContext();

        try (Spi spi = ctx.create(
                Spi.newConfigBuilder(ctx)
                        .bus(SpiBus.BUS_0)      // SPI0
                        .chipSelect(cs)
                        .baud(5_000_000)
                        .mode(SpiMode.MODE_0)
                        .build())) {

            byte[] rx = new byte[4];
            spi.read(rx);
            return decodeMax31855(rx);

        } finally {
            ctx.shutdown();
        }
    }


    /**
     * Hardware SPI0 (SCLK=GPIO11, MISO=GPIO9) but manual CS on any GPIO (e.g., GPIO16).
     */
    public static double readTemp_HwSPI_ManualCS(int csGpio) {

        Context ctx = Pi4J.newAutoContext();
        DigitalOutput cs = null;
        Spi spi = null;

        try {
            cs = ctx.create(DigitalOutput.newConfigBuilder(ctx)
                    .address(csGpio)
                    .initial(DigitalState.HIGH)   // idle HIGH (inactive)
                    .shutdown(DigitalState.HIGH)
                    .id("cs-" + csGpio)
                    .name("CS GPIO" + csGpio)
                    .build());

            spi = ctx.create(Spi.newConfigBuilder(ctx)
                    .bus(SpiBus.BUS_0)               // SPI0: CLK=GPIO11, MISO=GPIO9
                    .chipSelect(SpiChipSelect.CS_1)  // prevent CE0/CE1 toggling
                    .baud(5_000_000)
                    .mode(SpiMode.MODE_0)
                    .build());

            byte[] rx = new byte[4];

            cs.low();        // assert CS (active LOW)
            spi.read(rx);    // read 32-bit frame
            cs.high();       // deassert CS

            return decodeMax31855(rx);

        } finally {
            try { if (spi != null) spi.close(); } catch (Exception ignored) {}
            try { if (cs  != null) cs.high(); } catch (Exception ignored) {}
            ctx.shutdown();
        }
    }

    /** Shared decode: returns thermocouple temperature (°C) or throws on fault. */
    private static double decodeMax31855(byte[] rx) {
        int raw = ByteBuffer.wrap(rx).getInt();

        // Fault bit D16; if set, inspect D2..D0
        if ((raw & 0x00010000) != 0) {
            boolean scv = ((raw & 0x00000004) != 0);  // short to VCC
            boolean scg = ((raw & 0x00000002) != 0);  // short to GND
            boolean oc  = ((raw & 0x00000001) != 0);  // open circuit
            throw new IllegalStateException(String.format(
                    "MAX31855 fault: SCV=%b SCG=%b OC=%b (raw=0x%08X)", scv, scg, oc, raw));
        }

        // External thermocouple temp: bits 31..18, signed, 0.25 °C/LSB
        int ext14 = (raw >> 18) & 0x3FFF;
        if ((ext14 & 0x2000) != 0) ext14 |= ~0x3FFF;   // sign-extend
        return ext14 * 0.25;
    }

    //  /** No "throws Exception" — errors handled internally */
    // public static void main(String[] args) {
    //     jl_print("temperature read!");
    //     for (int i = 0; i < 5; i++) {
    //         try {
    //             // Manual CS on GPIO16
    //             double t_gpio16 = readTemp_HwSPI_ManualCS(16);

    //             // Hardware CS on CE0 (GPIO8)
    //             double t_gpio8  = readTemp_HwCS(8);

    //             System.out.printf("CE0(GPIO8) = %.2f °C%n", t_gpio8);
    //             System.out.printf("GPIO16     = %.2f °C%n%n", t_gpio16);

    //             Thread.sleep(1500);

    //         } catch (Exception e) {
    //             System.out.println("Error: " + e.getMessage());
    //         }
    //     }
    // } 
    
}
