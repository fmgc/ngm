import csv
import math

def stats(x):
	n = len(x)
	s = sum(x)
	m = float(s)/n
	d = sum( [(xi - m) ** 2 for xi in x])
	v = math.sqrt( d/(n-1))
	return (m,v)

with open("results.csv") as csvfile:
	results = csv.reader(csvfile, delimiter = ",")
	next(csvfile)
	data = dict()
	for row in results:
		row = map(lambda x: x.replace(" ",""), row)
		key = "-".join(reversed(row[:2]))
		if not key in data.keys():
			data[key] = list()

		data[key].append( map(lambda x: float(x), row[2:]) )
summary = dict()
for key in data.keys():
	golds = map(lambda x: x[0], data[key])
	times = map(lambda x: x[1], data[key])
	gs = stats(golds)
	ts = stats(times)
	summary[key] = gs + ts
with open("summary.csv","wb") as csvfile:
	summw = csv.writer(csvfile, delimiter = ",")
	summw.writerow(["noise", "per_corr", "golds_m", "golds_s", "time_m", "time_s"])
	for k in summary.keys():
		n,p = k.split("-")
		summw.writerow([n,p]+list(summary[k]))
	csvfile.close()



