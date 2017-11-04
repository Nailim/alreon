#include <iostream>
#include <string>
#include <stdlib.h>
#include <string.h>
#include <sstream>
#include <iostream>
using namespace std;

int main(){

	int tab[10];
	string one = " one.wav";
	string zero = " zero.wav";
	string sox = "sox";

	tab[0]=1;
	tab[9]=0;

	for (int a=0; a<256; a++){

		int i=a;

		for (int j=1;j<9;j++){
			tab[j]=0;
		}
		
		if (i>=128){
			tab[1]=1;
			i=i-128;
		}
		
		if (i>=64){
			tab[2]=1;
			i=i-64;
		}
		
		if (i>=32){
			tab[3]=1;
			i=i-32;
		}
	
		if (i>=16){
			tab[4]=1;
			i=i-16;
		}
		
		if (i>=8){
			tab[5]=1;
			i=i-8;
		}
		
		if (i>=4){
			tab[6]=1;
			i=i-4;
		}
	
		if (i>=2){
			tab[7]=1;
			i=i-2;
		}

		if (i==1){
			tab[8]=1;
		}


	string komanda="";
	komanda = komanda + sox;
	
	for (int t=0; t<10; t++){
		if (tab[t]==1){
			komanda = komanda + one;
		}
		else{
			komanda = komanda + zero;
		}
	}
	
	komanda = komanda + " ";

	std::string s;
	std::stringstream out;
	out << a;
	s = out.str();	

	komanda = komanda +s;
	komanda = komanda + ".wav";

	out << komanda;

	cout << komanda << endl;

	std::string str = komanda;
	const char* psz = str.c_str();

	system (psz);
	}



	return 0;
}
