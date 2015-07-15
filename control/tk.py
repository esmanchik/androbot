import time
import sys
import socket

port = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
port.connect(("192.168.33.225", 8080))

def stop(port):
    port.send("GET /stop HTTP/1.0\r\n\r\n")

def forward(port):
    port.send("GET /forward HTTP/1.0\r\n\r\n")

def backward(port):
    port.send("GET /backward HTTP/1.0\r\n\r\n")

def right(port):
    port.send("GET /left HTTP/1.0\r\n\r\n")

def left(port):
    port.send("GET /right HTTP/1.0\r\n\r\n")
    
try:
   # Python2
    import Tkinter as tk
except ImportError:
    # Python3
    import tkinter as tk

def key(event): 
    stop(port)
    """shows key or tk code for the key"""
    if event.keysym == 'Escape':
        port.close()
        root.destroy()
    if event.char == event.keysym:
     # normal number and letter characters
        print( 'Normal Key %r' % event.char )
    elif len(event.char) == 1:
      # charcters like []/.,><#$ also Return and ctrl/key
        print( 'Punctuation Key %r (%r)' % (event.keysym, event.char) )
        if event.char == ' ':
            stop(port)
    else:
      # f1 to f12, shift keys, caps lock, Home, End, Delete ...
        print( 'Special Key %r' % event.keysym )
        if event.keysym == 'Up':
            forward(port)
        elif event.keysym == 'Down':
            backward(port)
        elif event.keysym == 'Left':
            left(port)
        elif event.keysym == 'Right':
            right(port)

root = tk.Tk()
print( "Press a key (Escape key to exit):" )
root.bind_all('<Key>', key)
# don't show the tk window
root.withdraw()
root.mainloop()
