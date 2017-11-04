#!/usr/bin/env python

import string

from socket import *
def sockconn():
	try:
		s=socket(AF_INET, SOCK_STREAM)
		s.connect(('192.168.0.206', 4646))
#		s.connect(('172.16.1.7', 4646))
		return s
	except Except, e:
		print e,"sdfsdfsdfs"
def main():
	smain=sockconn()
	file = '/dev/shm/gpsRealtime.kml'
	while 1:
		data = smain.recv(1024)
		print 'Received:', data
		
		values = data.split(";")
		
		speed = (string.atof(values[3]) * 3600 / 1000)
		range = ( ( speed / 100  ) * 350 ) + 650
		tilt = ( ( speed / 120 ) * 43 ) + 30
		heading = 0
		
		if speed < 10:
			range = 200
			tilt = 30
			heading = 0
		
		output = """<?xml version="1.0" encoding="UTF-8"?>
	<kml xmlns="http://earth.google.com/kml/2.2">
		<Placemark>
			<name>%s km/h - Anemoi</name>
			<description></description>
			<LookAt>
				<longitude>%s</longitude>
				<latitude>%s</latitude>
				<range>%s</range>
				<tilt>%s</tilt>
				<heading>%s</heading>
			</LookAt>
			<LineString>
				<extrude>1</extrude>
				<tessellate>1</tessellate>
				<altitudeMode>absolute</altitudeMode>
				<coordinates>%s,%s,%s</coordinates>
			</LineString>
			<Point>
				<extrude>1</extrude>
				<tessellate>1</tessellate>
				<altitudeMode>absolute</altitudeMode>
				<coordinates>%s,%s,%s</coordinates>
			</Point>
		</Placemark>
	</kml>""" % (speed,values[0],values[1],range,tilt,heading,values[0],values[1],values[2],values[0],values[1],values[2])

		f=open(file, 'w')
		f.write(output)
		f.close()
		
		#print output
		
if __name__ == "__main__": main() 
