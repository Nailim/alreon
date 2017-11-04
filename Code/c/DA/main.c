#include <avr/io.h>
#include <avr/interrupt.h>
#include <avr/pgmspace.h>
#include <util/delay.h>

#define vrednostTOP 27648

unsigned int counter= 0;

void initUart(){

	UBRRH=(25>>8);
	UBRRL=25;			// 9600
	UCSRB = (1<<RXEN)|(1<<TXEN);	// Enable rx tx
	UCSRC = (1<<URSEL)|(3<<UCSZ0);		// 8bit no parity 1 stop

}

unsigned char receive(){

  while ( !(UCSRA & (1<<RXC)) );

  return UDR;

}


int main(void) {
	initUart();

	DDRC = 0xFF;
	PORTC = 0x00;
	
	DDRD = 0x60;

	DDRB=0xFF;
	PORTB=0xFF;

	for(;;){

		unsigned char temp = receive();
		unsigned char pC=(temp>>2);
		PORTC = pC;
		PORTB = temp;

	}

	return 0;
}

