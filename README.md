# androbot
Telepresence robot made from Android phone controlled via HTTP protocol, STM32F0 Discovery or Atmega8 as a motor controller communicating with phone using Bluetooth or USB UART interface

- HttpUart is Android app that provides HTTP service to control robot and get images from camera
- chassis contains motor controller programs: robot.c for Atmega8 and stm32* project for corresponding Discovery board
- control contains various Python scripts to move robot using keyboard
- vision has Python scripts to obtain and process images from camera
