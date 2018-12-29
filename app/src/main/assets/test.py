from __future__ import division
print(division)

import sys
print(sys.path)
import zipfile
import os
print(os)
print(os.uname())

def testread(name) :
    text_file = open(name, "rt")
    read = text_file.readline()
    print(read)
    text_file.close()
    return(read)
