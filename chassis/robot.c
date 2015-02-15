#define F_CPU 1000000UL

#include<avr/io.h>
#include<util/delay.h>

#define low(port, pin) port &= ~_BV(pin);
#define high(port, pin) port |= _BV(pin);

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

main()
{
    int d, i, cm, max, maxi;
    DDRC = 0xff; /* All pins of port C are output */
    DDRD = 0x00; /* All pins of port D are input */
    PORTC = 0x1e;
    move(0, 0, 50);
    d = 1;
    while(1) {
        move(-1 * d, 1 * d, ANGLE);
        max = maxi = 0;
        for (i = 0; i < 3; i++) {
            if (i) {
                move(1 * d, -1 * d, ANGLE);
            }
            cm = get_range();
            if (max < cm && cm < TURNOUT_RANGE * 15) {
                max = cm;
                maxi = i;
            }
        }
        if (max < TURNOUT_RANGE) {
            move(1, -1, ANGLE * 4);
        } else {
            move(-1 * d, 1 * d, (2 - maxi) * ANGLE);
            while (1) {
                move(1, 1, 5);
                cm = get_range();
                if (TURNOUT_RANGE < cm && cm < max - 10) {
                    max = cm;
                } else {
                    break;
                }
            }
        }
        // move(0, 0, 30); // pause
        d *= -1;
    }
}
