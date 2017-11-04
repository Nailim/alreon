#!/usr/bin/env python
from socket import *
def sockconn():
	try:
		s=socket(AF_INET, SOCK_STREAM)
		s.connect(('192.168.0.101', 4646))
		#s.connect(('10.203.37.208', 5555))
		return s
	except Except, e:
		print e
def main():
	smain=sockconn()
	input = ''
	while 1:
		#while input != 'q': #user enters the q letter from keyboard to exit
		input=raw_input("enter message > ")
		smain.send(input + "\n\r")
		data = smain.recv(1024)
		print 'Received:', data 
if __name__ == "__main__": main() 
