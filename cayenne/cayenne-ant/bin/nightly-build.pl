#!/usr/bin/perl

#
# Cayenne automated build script. Performs checkout of Cayenne via anonymous CVS,
# compiles and runs unit tests. Can optionaly notify about the outcome via email
# as well as upload successful builds to the download server. Used primarily for
# nightly builds. Requires:
# 
#   1. A UNIX box with Perl and mail
#   2. Ant 1.6
#   3. JDK 1.4 and JDK 1.5
#   4. cvs
#   6. Entry in $HOME/.cayenne/connection.properties for "nightly-test"
#
# Command line:
#     nightly-build.pl -d svnroot [-u] [-n] [-m email@example.com] 
#            -u - upload build and test results to the server
#            -n - skip SVN checkout (used mostly for debugging)
#            -d - SVN path
#
# Crontab:
#
#     2 5 * * * export CVS_RSH=ssh; /fullpathto/nightly-build.pl -d http://svn.apache.org/repos/asf/incubator/cayenne/main/trunk/cayenne/ [-u] [-m email@example.com]  2>&1 > /dev/null
#

use strict;
use File::Path;
use File::Copy;
use Getopt::Std;
use Cwd;

# set environment dependent vars
my $base = "$ENV{'HOME'}/work/nightly";
my $jar_base = "$ENV{'HOME'}/lib";

# process command line
our ($opt_u, $opt_m, $opt_n, $opt_d);
getopts('unm:d:');
die_with_email("CVSROOT must be passed using -d option.") unless $opt_d;

# process classpath
my @cp_lines =  `find -L $jar_base -type f -name \"*.jar\"`;
chomp(@cp_lines); 
$ENV{'CLASSPATH'} = join(":", @cp_lines);

`mkdir -p $base`;

my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime();
$year = 1900 + $year;
$mon = 1 + $mon;
my $label = "$year-$mon-$mday";	
my $out_file = "$base/cayenne-nightly-$label.txt";
unlink $out_file if -f $out_file;

# Find JDK 1.5 and 1.4 installations...
my $jdk_15 = "/opt/java-1.5";
my $jdk_14 = "/opt/java-1.4";

die_with_email("No JDK1.5 installation at $jdk_15") unless -d $jdk_15;
die_with_email("No JDK1.4 installation at $jdk_14") unless -d $jdk_14;

my $cayenne_src = "$base/cayenne/build";

my $ant = "/usr/bin/ant";

die_with_email("No Ant installation at $ant") unless -f $ant;

# Upload path on the server
my $rel_path = "/var/sites/objectstyle/html/downloads/cayenne/nightly";

# print timestamp for log
print_line("\n\n===================================================\n");
print_line("Nightly build: $label\n");

# checkout source
get_source();

# build
chdir "$cayenne_src/cayenne/cayenne-ant" or die_with_email("Can't change to $cayenne_src/cayenne/cayenne-ant: $!\n");

my $version = release_label();

#
# Do JDK 1.4 Regression Testing
# -----------------------------
$ENV{'JAVA_HOME'} = $jdk_14;
run_command("$ant help");
run_command("$ant clean");
my $status = run_command("$ant test-1_4");
die_with_email("JDK 1.4 Build failed, return status: $status\n") if $status;

#
# Build for real with JDK 1.5
# -----------------------------
$ENV{'JAVA_HOME'} = $jdk_15;
run_command("$ant help");
$status = run_command("$ant clean");
die_with_email("Build failed, return status: $status\n") if $status;
$status = 
  run_command("$ant test -Dproject.version=$version -Dcayenne.test.connection=nightly-test -Dcayenne.test.report=true");
my $test_failure = $status; 

$status = run_command("$ant release -Dproject.version=$version");
die_with_email("Build failed, return status: $status\n") if $status;

# upload
if($opt_u) {
	# make remote upload directory
	$status = run_command("ssh www.objectstyle.org mkdir -p $rel_path/$label");
	die_with_email("Can't create release directory, return status: $status\n") if $status;
	
	# Upload test results no matter what
	my $test_reports = "build/tests-report-nightly-test";
        my $upload_dir = "www.objectstyle.org:$rel_path/$year-$mon-$mday";
	run_command("chmod -R 755 $test_reports");
	run_command("rsync -rltp -e ssh --delete --exclude='*.xml' $test_reports/ $upload_dir/reports");

	# Upload status information
	my $footer;
	if($test_failure) {
		$footer = "Nightly build failed some tests, see <a href=\"reports\">test reports</a> for details.";
	}
	else {
		$footer = "Nightly build passed all tests.";
	}
	run_command("ssh www.objectstyle.org 'echo \"$footer\" > $rel_path/$label/FOOTER.html'");
	

	# Upload build even if it failed... 
	my @gz_files = <dist/*.gz>;
	my $gz_file = $gz_files[0];

	die "Distribution file not found." unless @gz_files;
	$status = 
	run_command("scp $gz_files[0] $upload_dir/");
	die_with_email("Can't upload release, return status: $status\n") if $status;
}

die_with_email("Unit tests failed, return status: $status\n") if $test_failure;


print_line("====== SUCCESS\n");
success_email("Build Succeeded.");

sub get_source() {
	if($opt_n) {
		die_with_email("No existing cayenne checkout at $cayenne_src") 
                       unless -d "$cayenne_src/cayenne";
	}
	else {
		# Prepare checkout directory
		rmtree($cayenne_src, 0, 1) if -d $cayenne_src;
		mkpath($cayenne_src, 1, 0711) or die_with_email("Can't create build directory: $!\n");
		chdir $cayenne_src or die_with_email("Can't change to $cayenne_src: $!\n");

		my $status = run_command(
			" svn" . 
			" export" .
			" $opt_d");
		print_line("SVN checkout status: $status\n");
		die_with_email("SVN checkout failed, return status: $status\n") if $status;
	}
}

sub release_label() {
	open(DEFAULT_PROPS, "< default.properties") or die_with_email("Can't open default.properties: $!\n");
	while(<DEFAULT_PROPS>) {
		chomp;
		if(/^project\.version\s*=\s*(.+)$/) {

			# make sure RELEASE-NOTES with the right version exist
                       copy("../cayenne-other/release-notes/RELEASE-NOTES-$1.txt", 
                             "../cayenne-other/release-notes/RELEASE-NOTES-$1-$label.txt") 
                             or die_with_email("Can't copy RELEASE-NOTES: $!\n");
                       # copy UPGRADE
                       copy("../cayenne-other/release-notes/UPGRADE-$1.txt", 
                             "../cayenne-other/release-notes/UPGRADE-$1-$label.txt") 
                             or die_with_email("Can't copy UPGRADE: $!\n");
                        return "$1-$label"; 
		}
	}

	close(DEFAULT_PROPS);
	die "Can't find 'project.version' in default.properties";
}

sub run_command() {
	my $command = shift;
	print_line("# $command\n");
	return system("$command >> $out_file 2>&1");
}

sub print_line() {
	my $line = shift;
	open(COUT, ">> $out_file") or die_with_email("Can't append to $out_file");
	print COUT $line;
	close COUT;
}


sub success_email() {
	my $msg = shift;

	if($opt_m) {
		open(MAIL, "| mail -s 'Cayenne Build Succeeded ($mon/$mday/$year)' $opt_m") 
			or die  "Can't send mail: $!\n";
    
		print MAIL "\n";
		print MAIL "Message:\n\n";
		print MAIL "  $msg\n";
		close(MAIL);   
	}
}


sub die_with_email() {
	my $msg = shift;

 	if(open(COUT, ">> $out_file")) {
		print COUT $msg;
		close COUT;
	}	

	if($opt_m) {
		open(MAIL, "| mail -s 'Subject: Cayenne Build Failed ($mon/$mday/$year)' $opt_m") 
		or die  "Can't send mail: $!\n";
    
		print MAIL "\n";
		print MAIL "Error message:\n\n";
		print MAIL "  $msg\n";
		close(MAIL);
	}
	
	die $msg;
}


