avr-gcc -mmcu=atmega8 -Os robot.c -o robot.o
avr-objcopy -j .text -j .data -O ihex robot.o robot.hex
