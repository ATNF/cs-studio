** Build Process for SNS

The 'Basic EPICS' and 'SNS' versions as well as optional plugins are built
'headless' into a product and a P2 repository.

Web page is in a separate JEE Project.

** Create new version
- IN MANY PLACES! UPDATE VERSION NUMBERS IN ALL THESE:
  * settings.sh
  * org.csstudio.sns.product/plugin.xml
  * org.csstudio.sns.product/SNS_CSS.product
  * org.csstudio.basic.epics.product/SNS_CSS.product
  * orc.csstudio.sns.feature.*/feature.xml
- Whenever a plugin changes "enough", increment its version number.
- For deployment, inc. the *sns.feature* versions
- The product file has the feature versions hardcoded. Before Eclipse 3.5.1,
  I used the text editor to check/update it to version "0.0.0" which matched
  all versions but then there came https://bugs.eclipse.org/bugs/show_bug.cgi?id=279480

** Prerequisites
- Install Eclipse 3.5 and the matching 'RCP delta pack', obtained by looking
  for Eclipse, Downloads by topic, RCP, correct version, "Delta Pack".
  On my first attempt, versions didn't match and voila, it didn't work.
  The delta pack is needed for the headless build as well as cross-builds.
  0.  Unzip the delta pack into the IDE installation for the headless build.
  1.  Also extract the delta pack archive into its own directory on disk.
  2.  Open the Target Platform preferences (Window -> Preferences -> Plug-in Development -> Target Platform).
  3.  Edit the active target, add the delta pack directory (top level that contains features, plugins).

      
** Headless build
  # Edit the version number in settings.sh(!!), then run
  sh make.sh
  
  The result is
  1) build/buildRepo
     A repository with the new binaries.
     Copied to update site web server as ..../repo$VERSION,
     then see mk_repo.sh and mirror_repo.sh for combining such repos
     from various versions into one big repo.
  2) apps/* application binaries and source snapshot
     Also copied to update site.
  

** Example for manual build in IDE: Build Basic EPICS
Open org.csstudio.basic.epics.product/CSS.product.

Adjust versions of CSS.product and org.csstudio.basic.epics.product.

Export product:
Root dir: CSS_EPICS
Destination directory: /Kram/Eclipse/Workspace/org.csstudio.sns.updatesite/apps,
do not include source code,
check the "generate metadata" box,
export for Linux GTK/x86, macos x86, win32 x86.



* Setup on SNS Ctrls net
Download CSS Zip, or install from repository (see install.sh)

Use online update to add Optional SNS tools (alarm, MPS, ...)
 
- Add a startup file like below,
  soft-link it to /usr/local/bin/css
 
- Run once, enter the alarm RDB password via preference GUI
  
#!/bin/sh
#
# Start CSS
#
# kasemirk@ornl.gov

CSS=/usr/local/css/CSS_2.x.x
# Use the latest product (last by version number)
PROD=`ls -d $CSS/plugins/org.csstudio.sns.product_* | tail -1`
INI=$PROD/SNS_CCR.ini
OPTIONS=$PROD/debug_options.txt

LOGDIR=/usr/local/css/log/`hostname`
LOG=$LOGDIR/css.$$

# Limit PATH to minimum
export PATH="/usr/local/java/jdk1.5.0_17/bin:/usr/local/css/alarm_scripts:/bin:/usr/bin:/usr/local/bin"
# Don't 'inherid' any Java or LD_..PATH
export CLASSPATH=""
export LD_LIBRARY_PATH=""

# Assert there's a log dir
if [ ! -d $LOGDIR ]
then
   mkdir $LOGDIR
   chmod 777 $LOGDIR
fi

# Files created by CSS should be shareable
umask 0

# Allow core files
ulimit -c unlimited

# Run in logdir, pipe console to log file
(
cd $LOGDIR ;
$CSS/css -debug $OPTIONS -workspace_prompt $HOME/CSS-Workspaces/`hostname` -consoleLog -share_link /ade/css/Share -pluginCustomization $INI "$@" 2>&1
) >$LOG &


  
* Site log

HOST         UPDATE      Java         CSS    Comment
srv02        2010/05/13  jdk1.6       2.x.x  at 2.0.0 
testf1       2010/04/26  jdk1.6.0_17  1.2.0  Locally built jca.so from 1.0.21 source (g++ version issue). Oracle connection fails intermittently
accl1/2      2010/05/13  jdk1.5.0_17  2.x.x  at 2.0.0 (not used)
cf-ics-srv1  2010/04/26  jdk1.5.0_17  1.2.0
tgt-ics-srv1 2009/07/16  jdk1.5.0_07  1.0.19
Cryo         2009/07/?   jdk1.5.?     1.0.19 Steve


