#!/usr/bin/env python

import os, sys, datetime, operator

numArgs = len(sys.argv)

if numArgs == 1:
	rootDir = "/var/www/html/apps/opensha"
elif numArgs == 2:
	rootDir = sys.argv[1]
else:
	print "USAGE: " + sys.argv[0] + " [dir]"
	sys.exit(2)
   
if not os.path.isdir(rootDir):
	print rootDir + " is not a directory!"
	sys.exit(1)

class JarFile:
	
	def __init__(self, fileName):
		self.fileName = fileName
		split = fileName.split("-")
		splitLen = len(split)
		self.svn = int(split[splitLen-2])
		verSplit = split[splitLen-3].split(".")
		self.version = (int(verSplit[0]), int(verSplit[1]), int(verSplit[2]))
		
		dateStr = split[splitLen-1].split(".")[0]
		dateSplit = dateStr.split("_")
		year = int(dateSplit[0])
		month = int(dateSplit[1])
		day = int(dateSplit[2])
		self.date = datetime.date(year, month, day)
	
	def __str__(self):
		return self.fileName

def containsDate(jars, date):
	for jar in jars:
		if jar.date == date:
			return True
	return False

def containsVersion(jars, version):
	for jar in jars:
		if jar.version == version:
			return True
	return False

def processDir(dir):
	pathSplit = os.path.split(dir)
	isNightly = pathSplit[1] == "nightly"
	isDist = pathSplit[1] == "dist"
	if isNightly:
		print "handling nightly dir: " + dir
	elif isDist:
		print "handling dist dir: " + dir
	jarFiles = []
	for fileName in os.listdir(dir):
		path = dir + os.sep + fileName
		if os.path.isdir(path):
			processDir(path)
			continue
		if not fileName.endswith(".jar"):
			continue
		if not (isNightly or isDist):
			# this isn't a build folder
			continue
		try:
			jarFile = JarFile(fileName)
		except:
			print "incorrectly formatted jar: " + fileName
			continue
		jarFiles.append(jarFile)
	if len(jarFiles) == 0:
		return
	if isNightly:
		jarFiles.sort(key=operator.attrgetter('date', 'svn'), reverse=True)
	elif isDist:
		jarFiles.sort(key=operator.attrgetter('version', 'date', 'svn'), reverse=True)
	print "jars in dir '" + dir + "':"
	for jar in jarFiles:
		print jar.fileName
	firstJar = jarFiles[0]
	firstDate = firstJar.date
	jarsToKeep = []
	jarsToDelete = []
	for jar in jarFiles:
		if isNightly:
			daysOld = (firstDate - jar.date).days
			day = jar.date.day
			if daysOld <= 7: # keep it if it's less than 7 days old
				if not containsDate(jarsToKeep, jar.date):
					jarsToKeep.append(jar)
					continue
			if daysOld <= 30: # keep if it it's less than 30 days and from sunday
				if day in (1, 8, 15, 22, 29) and not containsDate(jarsToKeep, jar.date):
					jarsToKeep.append(jar)
					continue
			elif daysOld <= 365: # keep it if it > 30 days old and it's from the 1st of the month
				if day == 1 and not containsDate(jarsToKeep, jar.date):
					jarsToKeep.append(jar)
					continue
			# if we made it here, then just delete it
			jarsToDelete.append(jar)
		elif isDist:
			if containsVersion(jarsToKeep, jar.version):
				jarsToDelete.append(jar)
				continue
			jarsToKeep.append(jar)
	print "keeping:"
	for jar in jarsToKeep:
		print jar.fileName
	for jar in jarsToDelete:
		print "deleting: " + jar.fileName
		os.unlink(dir + os.sep + jar.fileName)
		

processDir(rootDir)