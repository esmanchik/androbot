import cv2

img = cv2.imread('camera.jpg')
rows,cols,rest = img.shape
M = cv2.getRotationMatrix2D((cols/2,rows/2),-90,1)
dst = cv2.warpAffine(img,M,(cols,rows))
cv2.imwrite('vertical.jpg', dst)
