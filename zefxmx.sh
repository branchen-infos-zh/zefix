#!/bin/sh

PRG_NAME=${0##*/}
PRG_NAME2=${PRG_NAME%.*}
JAVA_CMD="java -jar zefxmx/target/zefxmx-standalone.jar"

# Defaults
IN_DIR=
OUT_FILE="comps.json"
JSON_FILE=
NOGA_FILE=
GEO=

function usage {
    printf "%s. Usage:\n" $PRG_NAME2
    printf "%s [-o output-file] [-i input-dir | -j json] [-n noga-cats] [-g]\n" $PRG_NAME
    echo ""
    echo " Converts a zefix xml files to json or reparses noga for json. If <output-file>"
    echo " was not provided"
    echo " prints to stdout. A custom noga-cats specification file in json can"
    echo " be provided with the '-n' option."
    exit 1
}

# Reset in case getopts has been used previously in the shell.
while getopts :i:j:o:n:gh name
do
    case $name in
        h)  usage;;
        i)  IN_DIR=$OPTARG;;
    	j)  JSON_FILE=$OPTARG;;
        o)  OUT_FILE=$OPTARG;;
        n)  NOGA_FILE=$OPTARG;;
        g)  GEO="YES";;
        h)  usage;;
        :)  echo "Option -$OPTARG requires an argument. Type '$PRG_NAME -h' for help." >&2
            exit 1;;
    esac
done

if [[ -z "$IN_DIR" && -z "$JSON_FILE" ]]; then
    echo "Input dir is required. Type '$PRG_NAME -h' for help." >&2
    exit 1
fi

OPTS="-o $OUT_FILE"

if [ ! -z "$JSON_FILE" ]; then
    OPTS="$OPTS -j $JSON_FILE"
else
    OPTS="$OPTS -i $IN_DIR"
fi

if [ ! -z "$NOGA_FILE" ]; then
    OPTS="$OPTS -n $NOGA_FILE"
fi

if [ ! -z "$GEO" ]; then
    OPTS="$OPTS -g"
fi

$JAVA_CMD $OPTS
