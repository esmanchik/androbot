import cv2
import time
import socket
import os

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.connect(("192.168.1.3", 8080))

def command(c):
    server.send("GET /" + c + " HTTP/1.0\r\nUser-Agent: Control\r\n\r\n")

def stop():
    command("stop")

def forward():
    command("forward")

def backward():
    command("backward")

def right():
    command("left")

def left():
    command("right")

def vertical(img):
    rows,cols,rest = img.shape
    M = cv2.getRotationMatrix2D((cols/2,rows/2),-90,1)
    return cv2.warpAffine(img,M,(cols,rows))

path = "/usr/share/opencv/haarcascades/haarcascade_frontalface_default.xml"
classifier = cv2.CascadeClassifier(path)

def faces(img):
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    return classifier.detectMultiScale(gray, 1.3, 5)    
    
def lifecycle():
    os.system("curl -O 192.168.1.3:8080/camera.jpg")
    img = cv2.imread("camera.jpg")
    vi = vertical(img)
    fcs = faces(vi)
    for f in fcs:
        x,y,w,h = f
        ww = vi.shape[1];
        l = x
        r = x + w
        wl = ww / 2 - ww / 4;
        wr = ww / 2 + ww / 4;
        print(wl, l, r, wr)
        if r > wr:
            d = r - wr;
            s = d * 0.007
            print(d, s, "->")
            right()
            time.sleep(s)
        elif l < wl:
            d = wl - l;
            s = d * 0.005
            print("<-", s, d)
            left()
            time.sleep(s)
        elif w * 2 < ww:
            print("^")
            forward()
            time.sleep(0.3)
        stop()
            

def main():
    for i in range(1, 30):
        lifecycle()
        # time.sleep(0.5)

if __name__=='__main__':
    main()

