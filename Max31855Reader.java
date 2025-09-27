import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.spi.*;
import java.nio.ByteBuffer;

public class Max31855Reader {

    private final Context pi4j;
    private final Spi spi;

    public Max31855Reader() {
        pi4j = Pi4J.newAutoContext();

        SpiConfig config = Spi.newConfigBuilder(pi4j)
                .id("spi0-ce0")
                .name("SPI0 CE0")
                .bus(SpiBus.BUS_0)                 // SPI0
                .chipSelect(SpiChipSelect.CS_0)    // CE0 (GPIO8)
                .baud(5_000_000)                   // up to 5 MHz is fine for MAX31855
                .mode(SpiMode.MODE_0)              // CPOL=0, CPHA=0
                .build();

        spi = pi4j.create(config);
    }

    /** Read one 32-bit frame from MAX31855 and decode to Celsius. */
    public Reading readOnce() throws Exception {
        byte[] rx = new byte[4];
        spi.read(rx);
        int raw = ByteBuffer.wrap(rx).getInt();

        // Fault bit (D16). If set, fault flags in D2..D0 tell what’s wrong.
        boolean fault = ((raw & 0x00010000) != 0);
        boolean scv = ((raw & 0x00000004) != 0);   // short to VCC
        boolean scg = ((raw & 0x00000002) != 0);   // short to GND
        boolean oc  = ((raw & 0x00000001) != 0);   // open circuit

        // External thermocouple temperature: bits 31..18, signed, 0.25°C/LSB
        int ext14 = (raw >> 18) & 0x3FFF;          // 14 bits
        if ((ext14 & 0x2000) != 0) {               // sign extend if negative
            ext14 |= ~0x3FFF;
        }
        double tC = ext14 * 0.25;

        // (Optional) Internal reference temp: bits 15..4, signed, 0.0625°C/LSB
        int int12 = (raw >> 4) & 0x0FFF;
        if ((int12 & 0x0800) != 0) {
            int12 |= ~0x0FFF;
        }
        double tInternalC = int12 * 0.0625;

        return new Reading(tC, tInternalC, fault, scv, scg, oc, raw, rx);
    }

    public void shutdown() { pi4j.shutdown(); }

    public static class Reading {
        public final double thermocoupleC;
        public final double internalC;
        public final boolean fault, scv, scg, oc;
        public final int raw32;
        public final byte[] bytes;
        Reading(double tC, double iC, boolean f, boolean v, boolean g, boolean o, int raw, byte[] b) {
            thermocoupleC = tC; internalC = iC; fault = f; scv = v; scg = g; oc = o; raw32 = raw; bytes = b;
        }
        @Override public String toString() {
            String fs = fault ? String.format(" [FAULT scv=%b scg=%b oc=%b]", scv, scg, oc) : "";
            return String.format("T=%.2f°C (internal=%.2f°C) raw=0x%08X%s", thermocoupleC, internalC, raw32, fs);
        }
    }

    public static void main(String[] args) throws Exception {
        Max31855Reader r = new Max31855Reader();
        try {
            for (int i = 0; i < 5; i++) {
                Reading rd = r.readOnce();
                System.out.println(rd);
                Thread.sleep(500);
            }
        } finally {
            r.shutdown();
        }
    }
}
