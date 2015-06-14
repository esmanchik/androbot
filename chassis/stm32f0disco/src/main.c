#include "stm32f0xx_conf.h"

void SysTick_Handler(void) {
  static uint16_t tick = 0;

  switch (tick++) {
  	case 100:
  		tick = 0;
  		GPIOC->ODR ^= (1 << 8);
  		break;
  }
}

void delay (int a)
{
	volatile int i,j;

	for (i=0 ; i < a ; i++)
	{
		j++;
	}

	return;
}

void USART2_Init() {
    USART_InitTypeDef USART_InitStructure;
    GPIO_InitTypeDef GPIO_InitStructure;

    RCC_AHBPeriphClockCmd(RCC_AHBPeriph_GPIOA, ENABLE);
    RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART2,ENABLE);

    GPIO_PinAFConfig(GPIOA, GPIO_PinSource2, GPIO_AF_1);
    GPIO_PinAFConfig(GPIOA, GPIO_PinSource3, GPIO_AF_1);

    //Configure USART2 pins:  Rx and Tx ----------------------------
    GPIO_InitStructure.GPIO_Pin =  GPIO_Pin_2 | GPIO_Pin_3;
    GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
    GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF;
    GPIO_InitStructure.GPIO_OType = GPIO_OType_PP;
    GPIO_InitStructure.GPIO_PuPd = GPIO_PuPd_UP;
    GPIO_Init(GPIOA, &GPIO_InitStructure);

    //Configure USART2 setting:         ----------------------------
    USART_InitStructure.USART_BaudRate = 9600;
    USART_InitStructure.USART_WordLength = USART_WordLength_8b;
    USART_InitStructure.USART_StopBits = USART_StopBits_1;
    USART_InitStructure.USART_Parity = USART_Parity_No;
    USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;
    USART_InitStructure.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;
    USART_Init(USART2, &USART_InitStructure);

    USART_Cmd(USART2,ENABLE);
}

void send(uint8_t c) {
	while(USART_GetFlagStatus(USART2, USART_FLAG_TXE) == RESET);
	USART_SendData(USART2, c);
}

uint8_t recv() {
	while(USART_GetFlagStatus(USART2, USART_FLAG_RXNE) == RESET);
	return USART_ReceiveData(USART2) & 15;
}

#define GPIOS_COUNT 3

int main (void) {
    int i;
    uint8_t buf;
    uint32_t bsrrs[GPIOS_COUNT];
    GPIO_TypeDef *gpio, *gpios[] = { GPIOA, GPIOB, GPIOC };

    USART2_Init();

    RCC->AHBENR |= RCC_AHBENR_GPIOAEN;
    RCC->AHBENR |= RCC_AHBENR_GPIOBEN;
    RCC->AHBENR |= RCC_AHBENR_GPIOCEN;
    GPIOA->MODER = 0x555555a5; // PA2 and PA3 are used by USART2
    GPIOB->MODER = 0x55555555;
    GPIOC->MODER = 0x55555555;

    for (i = 0; i < GPIOS_COUNT; i++) {
        bsrrs[i] = 0;
    }
    while(1) {
        buf = recv();
        if (buf == 0x0e) {
            for (i = 0; i < GPIOS_COUNT; i++) {
                gpio = gpios[i];
                gpio->BSRR = bsrrs[i];
                bsrrs[i] = 0;
            }
            continue;
        }
        int gpio_id;
        uint32_t pin;
        gpio_id = buf - 0x0a;
        if (gpio_id < 0 ||
            gpio_id > GPIOS_COUNT - 1) continue;
        gpio = gpios[gpio_id];
        buf = recv();
        pin |= 1 << buf;
        buf = recv();
        if (!buf) pin <<= 16;
        bsrrs[gpio_id] |= pin;
    }

    return 0;
}
