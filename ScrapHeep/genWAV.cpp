#include <fstream>
//#include <iostream>
using namespace std;

int main () {
	
	ofstream outfile;
	
	char * name = new char [15];
	name[0] = 's'; name[1] = 'n'; name[2] = 'd'; name[3] = '0'; name[4] = '0'; name[5] = '0'; name[6] = '0'; name[7] = '0'; name[8] = '0'; name[9] = '0'; name[10] = '0'; name[11] = '.'; name[12] = 'r'; name[13] = 'a'; name[14] = 'w';
	
	char * buffer = new char [55];
	for (int i = 0; i < 5; i++) {
		buffer[i] = 230;
	}
	for (int i = 5; i < 10; i++) {
		buffer[i] = 25;
	}
	for (int i = 50; i < 55; i++) {
		buffer[i] = 230;
	}
	
	for (int a = 0; a < 256; a++){

		int i = a;
		
		for (int j = 3; j < 11; j++){
			name[j] = '0';
		}
		
		if (i >= 128){
			name[3] = '1';
			i = i-128;
			for (int i = 45; i < 50; i++) {
				buffer[i] = 230;
			}
		} else {
			for (int i = 45; i < 50; i++) {
				buffer[i] = 25;
			}
		}
		
		if (i >= 64){
			name[4] = '1';
			i = i-64;
			for (int i = 40; i < 45; i++) {
				buffer[i] = 230;
			}
		} else {
			for (int i = 40; i < 45; i++) {
				buffer[i] = 25;
			}
		}
		
		if (i >= 32){
			name[5] = '1';
			i = i-32;
			for (int i = 35; i < 40; i++) {
				buffer[i] = 230;
			}
		} else {
			for (int i = 35; i < 40; i++) {
				buffer[i] = 25;
			}
		}
	
		if (i >= 16){
			name[6] = '1';
			i = i-16;
			for (int i = 30; i < 35; i++) {
				buffer[i] = 230;
			}
		} else {
			for (int i = 30; i < 35; i++) {
				buffer[i] = 25;
			}
		}
		
		if (i >= 8){
			name[7] = '1';
			i = i-8;
			for (int i = 25; i < 30; i++) {
				buffer[i] = 230;
			}
		} else {
			for (int i = 25; i < 30; i++) {
				buffer[i] = 25;
			}
		}
		
		if (i >= 4){
			name[8] = '1';
			i = i-4;
			for (int i = 20; i < 25; i++) {
				buffer[i] = 230;
			}
		} else {
			for (int i = 20; i < 25; i++) {
				buffer[i] = 25;
			}
		}
	
		if (i >= 2){
			name[9] = '1';
			i = i-2;
			for (int i = 15; i < 20; i++) {
				buffer[i] = 230;
			}
		} else {
			for (int i = 15; i < 20; i++) {
				buffer[i] = 25;
			}
		}

		if (i >= 1){
			name[10] = '1';
			i = i-1;
			for (int i = 10; i < 15; i++) {
				buffer[i] = 230;
			}
		} else {
			for (int i = 10; i < 15; i++) {
				buffer[i] = 25;
			}
		}
		
		outfile.open (name,ofstream::binary);
		outfile.write (buffer,55);
		outfile.close();
	}	
	
	outfile.open ("snd.raw",ofstream::binary);
	
	for (int i = 0; i < 55; i++) {
		buffer[i] = 230;
	}
	outfile.write (buffer,55);
	
	outfile.close();
	
	//delete[] buffer;
	
	return 0;
}