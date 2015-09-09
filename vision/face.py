import cv2

img = cv2.imread('vertical.jpg')
path = "/usr/share/opencv/haarcascades/haarcascade_frontalface_default.xml"
classifier = cv2.CascadeClassifier(path)
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
faces = classifier.detectMultiScale(gray, 1.3, 5)
for (x,y,w,h) in faces:
    cv2.rectangle(img, (x, y), (x + w,y + h), (0, 255, 0), 2)
cv2.imwrite('face.jpg', img)
