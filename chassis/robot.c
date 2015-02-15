#define F_CPU 1000000UL

#include<avr/io.h>
#include<util/delay.h>

#define low(port, pin) port &= ~_BV(pin)
#define high(port, pin) port |= _BV(pin)

#define TRIG_PIN PC5
#define ECHO_PIN PD2

int get_range() {
    int i, echo;
    /* hack: switch port for some time */
    _delay_us(10);
    DDRC = 0xdf;
    _delay_us(10);
    DDRC = 0xff;
    PORTC = 0x1e;
    /**/
    _delay_us(10);
    // i = 80;
    high(PORTC, PC5);
    _delay_us(450);
    // while (i > 0) i--;
    low(PORTC, PC5);
    i = echo = 0;
    while(PIND & 4 == 0) i++;
    while(PIND & 4) echo++;
    return echo;
}

void move(int left_wheel, int right_wheel, int deciseconds) {
    uint8_t left_pin, right_pin;
    left_pin = right_pin = 0;
    if (left_wheel > 0) left_pin = PC1;
    else if (left_wheel < 0) left_pin = PC2;
    if (right_wheel > 0) right_pin = PC4;
    else if (right_wheel < 0) right_pin = PC3;
    if (left_pin) low(PORTC, left_pin);
    if (right_pin) low(PORTC, right_pin);
    while(deciseconds) {
        //high(PORTC, PC0);
        _delay_ms(50);
        //low(PORTC, PC0);
        _delay_ms(50);
        deciseconds--;
    }
    if (left_pin) high(PORTC, left_pin);
    if (right_pin) high(PORTC, right_pin);
}

#define ANGLE 5
#define TURNOUT_RANGE 750

#define FOSC F_CPU // Clock Speed
#define BAUD 9600

void USART_Init()
{
    /* Calculated as FOSC/16/BAUD-1 */
    unsigned int ubrr = 12; /* 5 on U2X = 0 */
    /* Set baud rate */
    UBRRH = (unsigned char)(ubrr>>8);
    UBRRL = (unsigned char)ubrr;
    UCSRA = 1<<U2X;
    /* Enable receiver and transmitter */
    UCSRB = (1<<RXEN)|(1<<TXEN);
    /* Set frame format: 8data, 2stop bit */
    //UCSRC = (1<<URSEL)|(1<<USBS)|(3<<UCSZ0);
    /* Set frame format: 8data, 1stop bit, no parity */
    UCSRC = (1<<URSEL)|(3<<UCSZ0);
    /* Set frame format: 6data, 1stop bit, no parity */
    //UCSRC = (1<<URSEL)|(1<<UCSZ0);
}

void USART_Transmit( unsigned char data )
{
    /* Wait for empty transmit buffer */
    while ( !( UCSRA & (1<<UDRE)) )
        ;
    /* Put data into buffer, sends the data */
    UDR = data;
}

unsigned char USART_Receive( void )
{
    /* Wait for data to be received */
    while ( !(UCSRA & (1<<RXC)) )
        ;
    /* Get and return received data from buffer */
    return UDR;
}

void send_int(unsigned int i) {
    USART_Transmit ( i & 0xf );
    USART_Transmit ( i >> 4 & 0xf );
    USART_Transmit ( i >> 8 & 0xf );
    USART_Transmit ( i >> 12 & 0xf );
}

void uart_ping( void )
{
    unsigned char b;
    DDRC = 0xff;
    PORTC = 0;
    USART_Init ();
    b = 0x09;
    while (1) {
        USART_Transmit(b);
        _delay_ms(1000);
        b++;
        /*b = 0x5a;
          USART_Transmit(b);
          _delay_ms(1000);
          /*b = USART_Receive();
          PORTC = b;
          _delay_ms(1000);
          PORTC = 0;
          _delay_ms(1000);*/
    }
}

#define FL_PIN PC1
#define BL_PIN PC2
#define BR_PIN PC3
#define FR_PIN PC4

void stop() {
    high(PORTC, FL_PIN);
    high(PORTC, FR_PIN);
    high(PORTC, BL_PIN);
    high(PORTC, BR_PIN);
}

void move_forward() {
    stop();
    low(PORTC, FL_PIN);
    low(PORTC, FR_PIN);
}

void move_backward() {
    stop();
    low(PORTC, BL_PIN);
    low(PORTC, BR_PIN);
}

void rotate_left() {
    stop();
    low(PORTC, BL_PIN);
    low(PORTC, FR_PIN);
}

void rotate_right() {
    stop();
    low(PORTC, FL_PIN);
    low(PORTC, BR_PIN);
}

main()
{
    unsigned char command;
    unsigned int range;
    DDRC = 0xff; /* All pins of port C are output */
    DDRD = 0x02; /* Only pin 1 of port D is output */
    PORTC = 0x1e; /* Don't measure, stop all motors */
    USART_Init();
    while(1) {
        command = USART_Receive();
        command &= 0xf;
        if (0 < command && command < 8) {
            /*
             * 7 - move forward
             * 3 - rotate right
             * 5 - rotate left
             * 6 - move backward
             * 1 - stop
             */
            switch (command) {
            case 3: rotate_right(); break;
            case 5: rotate_left(); break;
            case 6: move_backward(); break;
            case 7: move_forward(); break;
            default: stop();
            }
        }
        USART_Transmit(command | 0x10);
   }
}
