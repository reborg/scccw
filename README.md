# Simple Clojure +Components +Config +Web

This is the template I use to start other projects. Call me old-school but:

```
git clone http://github.com/reborg/scccw
mv scccw yourprjname
cd yourprjname

# replace all occurrences (upper/lower/camel case) inside files with your new project name.
for f in `grep -lr "" *` ; do sed "s.scccw.yourprjname.g" $f > temp; \mv temp $f; done; rm -rf temp
for f in `grep -lr "" *` ; do sed "s.SCCCW.YOURPRJNAME.g" $f > temp; \mv temp $f; done; rm -rf temp
for f in `grep -lr "" *` ; do sed "s.Scccw.Yourprjname.g" $f > temp; \mv temp $f; done; rm -rf temp

# rename all files
find . -iname '*scccw*' -depth -exec bash -c 'mv "$1" "${1//scccw/yourprjname}"' -- {} \;
```

Solves 95% of my problems. Then lein repl, (reset) and the system is up and running. Customize at will.

Known Limitations: yourprjname needs to be a single word, no spaces no punctuation.
