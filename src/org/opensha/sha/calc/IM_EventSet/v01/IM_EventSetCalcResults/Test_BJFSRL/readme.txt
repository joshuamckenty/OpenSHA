For a site near downtown Los Angeles (34.1, -118.2, w/ assumed Vs30=760), this directory tests the PGA curve generated from the code here with validation curves previously published in Figure 2a of Field et al. (Seismological Research Letters, Vol 76,Number 5, Sept/Oct 2005).

The code here was run with the following commands:

	java -jar IM_EventSetCalcTest.jar BJF_EventSetFiles

BJF_EventSetFiles - input directory where EventSet files for the BJF for Site with Location 34.1,-118.2 using PGA as IMT were located (This is not included in the package)

The file "BJF_Test_SRL.txt" has the raw data for the comparison, where the three curves are as follows:

	DATASET #1 - curve computed from the OpenSHA hazard-curve calculator (from publication above)

	DATASET #2 - curve computed from Frankel's fortran code  (from publication above)

	DATASET #3 - curve computed from the code here.

The files "BJF_Test_SRL.png" and "BJF_Test_SRL.pdf" show a plot of the three curves (the latter including a legend).  The solid red line is from the OpenSHA hazard-curve calculator (DATASET #1), the blue "X" are from Frankel's code (DATASET #2), and the black solid circles are from the IM_EventSetCalc code (DATASET #3).

The agreement is very good.
