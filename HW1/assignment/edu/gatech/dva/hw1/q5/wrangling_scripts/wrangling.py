"""
cse6242 s21
wrangling.py - utilities to supply data to the templates.

This file contains a pair of functions for retrieving and manipulating data
that will be supplied to the template for generating the table. """
import csv

def username():
    return 'pkubsad3'

def data_wrangling():
    with open('data/movies.csv', 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        table = list()
        # Feel free to add any additional variables
        ...
        
        # Read in the header
        for header in reader:
            break

        i=1
        # Read in each row
        for row in reader:

            table.append(row)
            if(i>=100):
                break
            i+=1
        
        # Order table by the last column - [3 points] Q5.b
        table.sort(key=lambda movie: float(movie[2]), reverse=True)
    
    return header, table

