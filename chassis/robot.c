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
    i = echo; // debug blink
    high(PORTC, PC0);
    while (i > 0) {
        _delay_ms(10);
        i -= 1000;
    }
    low(PORTC, PC0); // end of debug blink
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
