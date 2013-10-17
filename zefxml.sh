#!/bin/bash
#-x

# @@D 17.10.13
# Why a shell script?|
#
# Fetching an XML from zefix is as simple as a GET request -> curl is our
# friend here. No need to rewrite the same functionality in its own program.
# Also it's easy to randomize requests by time and origin (proxy) to hide
# traces so that the Zefix server won't block the ip that is performing the
# requests. One drawback is probably portability but, well, I can live with
# this.
# @@

# The url format to be used to query a company's Handelsregister-Eintrag
ZEFIX_URL="http://zh.powernet.ch/webservices/inet/hrg/hrg.asmx/getExcerpt?Chnr=%s&Amt=20&Lang=1"

CURL_BIN=`which curl`
CAT_BIN=`which cat`

# Initialize variables
iflag=
IDS_FILE=
oflag=
OUT_DIR=

PRG_NAME=${0##*/}
PRG_NAME2=${PRG_NAME%.*}

function usage {
    printf "%s. Usage:\n" $PRG_NAME2
    printf "%s -o output-directory [-i comp-ids-file] [args]\n" $PRG_NAME
    echo " Fetches Handelsregister excerpts as xml for a list of company ids from a"
    echo " file, if -i was provided, or, from the argument list, if option -i was"
    echo " omitted. Writes files to the directory provided by -o."
    exit 1;
}

# Make sure user provided arguments
if [[ $# -lt 1 ]]; then
    echo "Missing arguments. Type '$PRG_NAME -h' for help!"
fi

# First ':' -> omit getopts error messages
while getopts :o:i:h name
do
    case $name in
        i)  iflag=1
            IDS_FILE=$OPTARG;;
        o)  oflag=1
            OUT_DIR="$OPTARG";;
        h)  usage;;
        :)  echo "Option -$OPTARG requires an argument. Type '$PRG_NAME -h' for help!" >&2
            exit 1;;
    esac
done

# Get the reamining arguments
shift $(($OPTIND -1))
REM_ARGS=$*

echo "REM ARGS: $REM_ARGS"
echo "IDS FILE: $IDS_FILE"

if [ ! -d "$OUT_DIR" ]; then
    echo "Output directory provide by option '-d' does not exist or is not writable!"
    echo "Aborting..."
    exit 1
fi

if [[ ! -z "$iflag" && ! -z "$REM_ARGS" ]] \
    || [[ -z "$REM_ARGS" && ! -f "$IDS_FILE" ]]; then
    echo "Invalid combination of arguments: Either no ids were provided, the ids"
    echo "file provided by option '-i' does not exist, or both, arguments and"
    echo "ids file, were provided, or no ids. Type '$PRG_NAME -h' for help."
    echo "Aborting..."
    exit 1
fi

ids=$REM_ARGS
if [ -f "$IDS_FILE" ]; then
    ids=`$CAT_BIN $IDS_FILE`
fi

for id in $ids ; do
    url=`printf $ZEFIX_URL $id`
    out_file="$OUT_DIR/$id.xml"
    echo "Fetching xml for $id to $out_file..."
    echo " > $url"
    echo "$CURL_BIN --progress-bar $url -o $out_file"
done
