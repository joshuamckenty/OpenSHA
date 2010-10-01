This test compares our hazard curves for the SoCalEdison sites against two "official" points on the curve obtained from the USGS/NSHMP web site (http://eqint.cr.usgs.gov/eq-men/html/lookup-2002-interp-06.html).

The three sites were assumed to be Vs30=760 as necessary for the comparison.

The test was done for three IMTs (PGA, SA-1.0 , SA-0.2sec) and at the following three sites:

	34.0543, -118.0821	(General Office 2)
	33.6567, -117.7051	(Irvine Operations Center (IOC))
	34.0852, -118.1421	(Grid Control Center (GCC))

Our curves were computed with the following commands:

	java -jar IM_EventSetCalcTest.jar EventSetFiles

EventSetFiles - input directory where EventSet files for all the 4 AttnuationRelationhsips for each Site with Location( taken one at a time) , as defined above, using PGA, SA-1sec and SA-0.2sec as IMT ( taken one at a time), were located (This is not included in the package).

To be consistent with the USGS/NSHMP values, the curves for the four attenuation relationships (AS-1997, CB-2003, Sadigh-1997, BJF-1997),were averaged (equal weight) to get the final curve for comparison.

Here is an example of how we obtained the two "official" USGS/NSHMP points for each curve. We first entered the lat and lon for the location (e.g., 34.0543,  -118.0821) at http://eqint.cr.usgs.gov/eq-men/html/lookup-2002-interp-06.html and then hit "Submit", which yields:

-----------------------------------------------------------------
	LOCATION                 34.0543 Lat.  -118.0821 Long.
	The interpolated Probabilistic ground motion values, in %g,
	at the requested point are:
	            10%PE in 50 yr   2%PE in 50 yr
	   PGA         51.13            88.33
	0.2 sec SA     120.62            213.19
	1.0 sec SA     41.57            72.85
-----------------------------------------------------------------

We then convert the %g values to g (by dividing by 100), and then compute the annualized rates corresponding to 10%in50yrs and 2%in50yrs as:

	-ln(1-0.10)/50 = 0.00211
	-ln(1-0.02)/50 = 4.04e-4

Thus, for this example, the two points on the curve for PGA are:

	IML (x-axis)	rate (y-axis)        
	0.5113		0.00211	    
	0.8833		4.04e-4     

This process was repeated for all three sites and all three IMTs, giving a total of 9 comparisons.

The comparison files have prefix names defined as:

	Site-Lat + "_" + Site-Lon + "_" + IMT 

For example, the case where the site location is (34.0543 , -118.1421) and the IMt is PGA will have the following prefix: "34.0543_-118.1421_PGA"

Files with the ".txt" suffix have the raw data, and the ".png" and ".pdf" files are the comparison plots (the latter with a legend).  The solid red line in all the plots is the rate curve computed here, and the two black "X" symbols are the values from the USGS web site.

The comparisons are good, although differences of up to ~20% are present.

These differences result from how the background/gridded sources are treated between OpenSHA and Frankel's Fortran code (supported by the fact that the greatest difference is at the site (33.6567, -117.7051) where the background seismicity is most influential, as can be confirmed by using the USGS disaggregation tool).

The differences in how the background seismicity is treated are as follows:

1) Each grid-point rupture is given a finite, straight, vertical rupture surface with a randomly assigned strike.  This random strike inevitably differs between OpenSHA and Frankel's Fortran code.

2) OpenSHA treats the grid sources as strike-slip events, whereas some of the USGS/NSHMP grid sources are given some percentage of thrust events (which generally increases the IMLs in the latter).

3) The exact distance cutoff differs between the two, and this can have a slight influence.

Note that these differences are small compared to the many sources of epistemic uncertainty in these calculations (e.g., curves from the four different attenuation relationships).  Therefore, if you worry about the differences here, you should also be very concerned about all those other epistemic uncertainties.
