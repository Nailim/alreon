#! /bin/sh

for i in `seq 0 255`; do
	
	b="00000000"
	c=$i
	
	if [ $i -ge 128 ]; then
		b="1`expr substr $b 2 7`"
		i=$(( $i - 128 ))
	fi
	
	if [ $i -ge 64 ]; then
		b="`expr substr $b 1 1`1`expr substr $b 3 6`"
		i=$(( $i - 64 ))
	fi
	
	if [ $i -ge 32 ]; then
		b="`expr substr $b 1 2`1`expr substr $b 4 5`"
		i=$(( $i - 32 ))
	fi
	
	if [ $i -ge 16 ]; then
		b="`expr substr $b 1 3`1`expr substr $b 5 4`"
		i=$(( $i - 16 ))
	fi
	
	if [ $i -ge 8 ]; then
		b="`expr substr $b 1 4`1`expr substr $b 6 3`"
		i=$(( $i - 8 ))
	fi
	
	if [ $i -ge 4 ]; then
		b="`expr substr $b 1 5`1`expr substr $b 7 2`"
		i=$(( $i - 4 ))
	fi
	
	if [ $i -ge 2 ]; then
		b="`expr substr $b 1 6`1`expr substr $b 8 1`"
		i=$(( $i - 2 ))
	fi
	
	if [ $i -ge 1 ]; then
		b="`expr substr $b 1 7`1"
	fi
	
	#echo "InputStream inputStream$b;"
	#echo "sndBufferSample = new byte[sampleLenght];"
    echo "inputStream = getResources().openRawResource(R.raw.snd$b);"
	echo "inputStream.read(sndSamples[$c]);"
	#echo "sndSamples[$c] = sndBufferSample;"
	echo "inputStream.close();"
	echo ""
done	
