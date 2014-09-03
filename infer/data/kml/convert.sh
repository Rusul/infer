


   for file in *.kml 
	do
		gpsbabel -i kml -f $file -o gpx -F $file.gpx
	done


