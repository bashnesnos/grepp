WORK_DIR=$(pwd)
alias wgrep='java -Dwgrep.home=$WORK_DIR -cp "$WORK_DIR/wgrep.jar:$WORK_DIR" org.smlt.tools.wgrep.WGrep $WORK_DIR/config.xml'
