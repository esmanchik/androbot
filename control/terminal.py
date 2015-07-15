import time
import socket
import sys,tty,termios

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.connect(("192.168.33.225", 8080))

def stop():
    server.send("GET /stop HTTP/1.0\r\n\r\n")

def forward():
    server.send("GET /forward HTTP/1.0\r\n\r\n")

def backward():
    server.send("GET /backward HTTP/1.0\r\n\r\n")

def right():
    server.send("GET /left HTTP/1.0\r\n\r\n")

def left():
    server.send("GET /right HTTP/1.0\r\n\r\n")

def main():
    fd = sys.stdin.fileno()
    old_settings = termios.tcgetattr(fd)
    def say(msg):
        termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)
        print(msg)
        tty.setraw(sys.stdin.fileno())
        
    try:
        tty.setraw(sys.stdin.fileno())
        while True:
            ch = sys.stdin.read(1)
            if ch == '\x1b':
                key = sys.stdin.read(2)
                if key == '[A':
                    say('up')
                    stop()
                    forward()
                elif key == '[B':
                    say('down')
                    stop()
                    backward()
                elif key == '[C':
                    say('right')
                    stop()
                    right()
                elif key == '[D':
                    say('left')
                    stop()
                    left()

            else:
                say("%s: %02x" % (ch, ord(ch)))
                if ch == ' ':
                    stop()
                elif ch == 'q':
                    stop()
                    termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)
                    exit(0)
    finally:
        termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)

if __name__=='__main__':
    main()
