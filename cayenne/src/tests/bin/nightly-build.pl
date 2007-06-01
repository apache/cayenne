#!/usr/bin/perl

#
# Cayenne automated build script. Performs checkout of Cayenne via anonymous CVS,
# compiles and runs unit tests. Can optionaly notify about the outcome via email
# as well as upload successful builds to the download server. Used primarily for
# nightly builds. Requires:
# 
#   1. A UNIX box with Perl 
#   2. Ant 1.5
#   3. JDK 1.4
#   4. cvs
#   5. qmail
#   6. Entry in $HOME/.cayenne/connection.properties for "nightly-test"
#
# Command line:
#     nightly-build.pl -d cvsroot [-u] [-n] [-m email@example.com] 
#            -u - upload build and test results to the server
#            -n - skip CVS checkout (used mostly for debugging)
#            -d - CVSROOT to use for code checkout
#
# Crontab:
#
#     2 5 * * * export CVS_RSH=ssh; /fullpathto/nightly-build.pl -d :ext:xyz@cvs.sourceforge.net:/cvsroot/cayenne [-u] [-m email@example.com]  2>&1 > /dev/null
#

use strict;
use File::Path;
use File::Copy;
use Getopt::Std;
use Cwd;


# These must be defined as environment variables
# (May need to modify script to make this configurable on the command line)
$ENV{'JAVA_HOME'} = "/opt/java";
$ENV{'ANT_HOME'} = "/opt/ant";

our ($opt_u, $opt_m, $opt_n, $opt_d);
getopts('unm:d:');

die_with_email("CVSROOT must be passed using -d option.") unless $opt_d;

my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime();
$year = 1900 + $year;
$mon = 1 + $mon;
my $label = "$year-$mon-$mday";	
my $out_file = "/tmp/cayenne-nightly-$label.txt";
unlink $out_file if -f $out_file;

die_with_email("No JDK1.4 installation at $ENV{'JAVA_HOME'}") unless -d $ENV{'JAVA_HOME'};

my $cayenne_src = "/tmp/cayenne/build";
my $ant = "$ENV{'ANT_HOME'}/bin/ant";
die_with_email("No Ant installation at $ant") unless -f $ant;

# Upload path on the server
my $rel_path = "/var/sites/objectstyle/html/downloads/cayenne/nightly";

# print timestamp for log
print_line("\n\n===================================================\n");
print_line("Nightly build: $label\n");

# checkout source
get_source();

# build
chdir "$cayenne_src/cayenne" or die_with_email("Can't change to $cayenne_src/cayenne: $!\n");

set_release_label();

my $status = run_command("$ant clean");
die_with_email("Build failed, return status: $status\n") if $status;

$status = run_command("$ant release");
die_with_email("Build failed, return status: $status\n") if $status;


# unit tests - ant
$status = 
  run_command("$ant test -Dcayenne.test.connection=nightly-test -Dcayenne.test.report=true");
my $test_failure = $status; 

# upload
if($opt_u) {
	# make remote upload directory
	$status = run_command("ssh www.objectstyle.org mkdir -p $rel_path/$label");
	die_with_email("Can't create release directory, return status: $status\n") if $status;
	
	# Upload test results no matter what
	my $test_reports = "build/tests/report/nightly-test";
        my $upload_dir = "www.objectstyle.org:$rel_path/$year-$mon-$mday";
	run_command("chmod -R 755 $test_reports");
	run_command("rsync -rltp -e ssh --delete --exclude='*.xml' $test_reports/ $upload_dir/reports");
	

	# Upload build if it succeeded
	if(! $test_failure) {
		my @gz_files = <dist/*.gz>;
		my $gz_file = $gz_files[0];

		die "Distribution file not found." unless @gz_files;
		$status = 
		run_command("scp $gz_files[0] $upload_dir/");
		die_with_email("Can't upload release, return status: $status\n") if $status;
	}
	
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

		my $status = 0;
                my $i = 0;
                 
                # cvs checkouts are unreliable...give it a few tries
		$ENV{"CVS_RSH"} = "ssh";
		for($i = 0; $i < 5; $i++) {
                	$status = run_command(
			" cvs" . 
			" -z3" .
			" -q" .
			" -d$opt_d" .
			" export" .
			" -D" .
			" \"1 minute ago\"" .
			" cayenne");
                    print_line("CVS checkout status: $status\n");
                    last unless $status;
                    sleep 120;
                }
		die_with_email("CVS checkout failed, return status: $status, attempts: $i\n") if $status;
	}
}

sub set_release_label() {
	open(DEFAULT_PROPS, "< default.properties") or die_with_email("Can't open default.properties: $!\n");
        open(LABELED_PROPS, "> build.properties") or die_with_email("Can't open build.properties: $!\n"); 
	while(<DEFAULT_PROPS>) {
		chomp;
		if(/^project\.version\s*=\s*(.+)$/) {
                        my $version = "$1-$label"; 
			
			# copy RELEASE-NOTES
			copy("doc/release-notes/RELEASE-NOTES-$1.txt", 
                             "doc/release-notes/RELEASE-NOTES-$version.txt") 
                             or die_with_email("Can't copy RELEASE-NOTES: $!\n");	
			# copy UPGRADE
			copy("doc/upgrade/UPGRADE-$1.txt", 
                             "doc/upgrade/UPGRADE-$version.txt") 
                             or die_with_email("Can't copy UPGRADE: $!\n");	


			print LABELED_PROPS "project.version = $version\n";
		}
		else {
			print LABELED_PROPS "$_\n";
		}
	}

	close(DEFAULT_PROPS);
	close(LABELED_PROPS);
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


