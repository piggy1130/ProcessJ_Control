import spidev
spi = spidev.SpiDev()
spi.open(0, 0)                # bus 0, CE0
spi.max_speed_hz = 5000000
resp = spi.xfer2([0,0,0,0])
print(resp)                   # expect 4 non-zero bytes if device present
