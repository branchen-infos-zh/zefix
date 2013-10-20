#!/bin/sh

PRG_NAME=${0##*/}
PRG_NAME2=${PRG_NAME%.*}
JAVA_CMD="java -jar zefxmx/target/zefxmx-standalone.jar"

# Defaults
IN_DIR=
OUT_FILE="comps.json"

function usage {
    printf "%s. Usage:\n" $PRG_NAME2
    printf "%s [-o output-file] [-i input-dir]\n" $PRG_NAME
    echo ""
    echo " Converts a zefix xml files to json. If <output-file> was not provided"
    echo " prints to stdout."
    exit 2
}

# Reset in case getopts has been used previously in the shell.
while getopts :i:o:h name
do
    case $name in
        h)  usage;;
        i)  IN_DIR=$OPTARG
        o)  OUT_FILE=$OPTARG;;
        h)  usage;;
        :)  echo "Option -$OPTARG requires an argument. Type '$PRG_NAME -h' for help." >&2
            exit 1;;
    esac
done

#echo $JAVA_CMD -o \""$OUT_FILE"\" -p \""$PATTERN"\" -r \""$RECHTSF"\" -n "$NUM"
$JAVA_CMD -o "$OUT_FILE" -i "$IN_DIR"
