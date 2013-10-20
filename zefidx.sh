#!/bin/sh

PRG_NAME=${0##*/}
PRG_NAME2=${PRG_NAME%.*}
JAVA_CMD="java -jar zefidx/target/zefidx-standalone.jar"

# Defaults
PATTERN=
NUM="1000"
RECHTSF=""
OUT_FILE="comp.ids"

function usage {
    printf "%s. Usage:\n" $PRG_NAME2
    printf "%s [-o output-file] [-n num-records] [-l] [-r rechtsform] -p pattern\n" $PRG_NAME
    echo ""
    echo " Fetches company ids from the zefix webservice and stores the output to stdout"
    echo " or, if <output-file> was provided, to <output-file>."
    echo ""
    echo " Options:"
    echo " -p  The company name pattern to query. At least one letter."
    echo " -n  The number of company ids to be fetched."
    echo " -r  Limit the Rechtsform of companies to be queried. Use -l to "
    echo "     get a list of valid ids."
    echo " -l  List available Rechtsformen ids"
    echo " -o  Output ids to that file. If omitted prints to stdout."
    echo " -h  Prints this help."
    exit 2
}

# Reset in case getopts has been used previously in the shell.
while getopts :lp:n:r:o:h name
do
    case $name in
        h)  usage;;
        l)  $JAVA_CMD --list-rf;;
        p)  PATTERN=$OPTARG;;
        n)  NUM=$OPTARG;;
        r)  RECHTSF=$OPTARG;;
        o)  OUT_FILE=$OPTARG;;
        h)  usage;;
        :)  echo "Option -$OPTARG requires an argument. Type '$PRG_NAME -h' for help." >&2
            exit 1;;
    esac
done

if [ -z "$PATTERN" ]; then
    echo "Pattern required (option '-p'). Aborting..."
    exit 1
fi

#echo $JAVA_CMD -o \""$OUT_FILE"\" -p \""$PATTERN"\" -r \""$RECHTSF"\" -n "$NUM"
$JAVA_CMD -o "$OUT_FILE" -p "$PATTERN" -r "$RECHTSF" -n "$NUM"

