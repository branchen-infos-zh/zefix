#!/bin/bash
#-x

PRG_NAME=${0##*/}
PRG_NAME2=${PRG_NAME%.*}
JAVA_CMD="java -jar zefidx/target/zefidx-standalone.jar"
iflag=
IDS_FILE=
oflag=
OUT_DIR=

ZEFIX_URL="http://zh.powernet.ch/webservices/inet/hrg/hrg.asmx/getExcerpt?Chnr=%s&Amt=20&Lang=1"

function usage {
    printf "%s. Usage:\n" $PRG_NAME2
        printf "%s -o output-directory [-i comp-ids-file] [args]\n" $PRG_NAME
            echo " Fetches Handelsregister excerpts as xml for a list of company ids from a"
                echo " file, if -i was provided, or, from the argument list, if option -i was"
                    echo " omitted. Outputs to the directory provided by -o."
                        exit 2
                        }
                        
                        # Reset in case getopts has been used previously in the shell.
                        while getopts :o:i:? name
                        do
                            case $name in
                                    i)  iflag=1
                                                IDS_FILE=$OPTARG;;
                                                        o)  oflag=1
                                                                    OUT_DIR="$OPTARG";;
                                                                            \?)  usage;;
                                                                                    :)  echo "Option -$OPTARG requires an argument." >&2
                                                                                                exit 1;;
                                                                                                    esac
                                                                                                    done