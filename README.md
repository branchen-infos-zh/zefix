Zefix
===
Zefix is a collection of scripts and clojure programs that can query
the zefix - Zentraler Firmenindex - webservice, the official swiss company
registry, and convert them into JSON. This was developed as part of the
Open Data hacknights in late 2013, see http://make.opendata.ch/wiki/project:biz.

Dependencies
---
- java
- maven
- leiningen, for installation instructions see https://github.com/technomancy/leiningen#installation
- bash

Building
---
1. Make sure the leiningen is installed (see dependencies)
2. Checkout the source, if you haven't done so already
3. `cd` into the directory, e.g. `cd zefix`
4. Run `./build.sh`. This will compile *zefidx* and *zefxmx*

Running
---
If you wish to fetch company ids use `zefidx` to do so. Type `zefidx.sh -h` for information on how to invoke the command.

Once you've fetched ids you can download the xmls with `zefxml.sh`. Again, type `zefxml.sh -h` for help. This will place the xmls in a folder for later use.

Use `zefxmx.sh` to process the xmls, geo-code the addresses and output the data as json. Typing `zefxmx.sh -h` will show you all available options.

Limitations
---
- For now this works only for zefix data queried from *Handelsregisteramt des Kantons Zürich*,
  see http://zefix.admin.ch/info/ger/zh020.htm.
- The zefix server might block your ip if you query to much data. Bulk queries are not supported.
  This is especially true, when fetching xmls with `zefxml.sh`.
- The geocoding will fail for the same reason above: too many queries. This is though temporarily
  and can be retried at some point later.
  Question: Can we circumvent this issue by using http://open.mapquestapi.com/nominatim/, as there
  may not be any usage limit, or following
  http://wiki.openstreetmap.org/wiki/Nominatim_usage_policy#Bulk_Geocoding?
- Fault tolerance is so so. In nasty cases the script has to be rerun. It does not pick up where it crashed, unfortunately.

Team
---
TODO

License
---
TODO
