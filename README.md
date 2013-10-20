Zefix
===
Zefix is a collection of scripts and clojure programs that can query
the zefix - Zentraler Firmenindex - webservice, the official swiss company
registry, and convert them into JSON. This was developed as part of the
Open Data hacknights in late 2013.

Scripts
---
zefidx.sh   Retrieves company ids from the zefix webservice.
zefxml.sh   Fetches a Handelsregister Eintrag from the zefix webservice.
zefxmx.sh   Convert a zefix xml (or a folder thereof) to json.

Recommended workflow
---
1) Fetch company ids with zefidx
2) Retrieve Handelsregister-Einträge with zefxml
3) Convert the xmls to json with zefxmx

Team
---
TODO

License
---
TODO
