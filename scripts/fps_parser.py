import numpy as np
import matplotlib.pyplot as plt
import sys

"""
  This script helps in parsing fps information given in a file and in drawing the graph
  using matplotlib, x axis is in seconds, y axis is in frames per second.
  This script assumes that the input file contains comma (",") separated fps info.
"""
# Function to read a file and split the data into x, y.
def getDataFromFile(filename, x, y):
    with open(filename, 'r') as f:
        data = f.read()

    data = data.split(',')
    i=0
    for row in data:
      if row != '':
        y.append(int(row))
        i +=1
        x.append(i)

# list file names from command line and read comma separated fps data.
labels = []
for filename in sys.argv[1:]:
    x = []
    y = []
    getDataFromFile(filename, x, y)
    plt.plot(x, y)
    labels.append(filename.upper().split(".")[0])

plt.title('Encoder performance')
plt.xlabel('time (s)')
plt.ylabel('Frames')
plt.legend(labels, loc='upper right')
plt.show()
