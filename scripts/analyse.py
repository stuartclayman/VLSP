#!/usr/bin/env python

import sys,copy
import numpy

def printUsage():
  print >> sys.stderr,"Usage: analyse.py [file] [lines]"
  sys.exit(-1)

args= sys.argv
if len(args) < 2:
    printUsage()
filename= args[1]
f= open(filename,'r')
line= f.readline()
data= {}
while line != "":
  nums= line.split()
  index= float(nums[0])
  dataline= []
  dataline.append(1.0)
  for i in nums:
    dataline.append(float(i))
  
  if index in data:
    olddataline= data[index]
    #print "OLD DATA",index,olddataline
    newdataline= list(olddataline)
    newdataline.append(list(dataline))
    data[index]= newdataline
  else:
    #print "NEW DATA",index,[dataline]
    data[index]= [list(dataline)]
    dataline=None
    #print "TRY",data[index]
  line= f.readline()
f.close()
for k in sorted(data.keys()):
  print k,
  dataline= data[k]
  for i in range(len(dataline[0])):
    series=[]
    for d in dataline:
      series.append(d[i])
    print numpy.mean(series),
    if len(series) == 1:
      print 0.0,
    else:
      print numpy.std(series),
  print
