import serial # pip install pyserial
import time
import sys

def turn(s, d, t):
    code = "\x05" if d == "left" else "\x03"
    s.write(code)
    time.sleep(t)
    s.write("\x01")

def main():
    d = sys.argv[1] if len(sys.argv) > 1 else "left"
    t = float(sys.argv[2]) if len(sys.argv) > 2 else 0.15
    s = serial.Serial('/dev/ttyUSB0')
    turn(s, d, t)
    s.close()
    
if __name__=='__main__':
    main()
        
