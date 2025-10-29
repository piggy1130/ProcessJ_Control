import spidev
spi = spidev.SpiDev()
spi.open(0,0)
spi.max_speed_hz = 500000
print(spi.xfer2([1,2,3,4]))
