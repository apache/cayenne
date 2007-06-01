#!/usr/bin/perl

#
# Cayenne script for taking CVS snapshots. 
# nightly builds. Requires:
# 
#   1. A UNIX box with Perl 
#   2. cvs
#   3. qmail
#
# Command line:
#     cvs-snapshot.pl -c checkoutfolder [-l label] [-m email@example.com] 
#            -c - checkout folder that should already contain "cayenne" and "sandbox"
#                 modules checked out from CVS
#            -l - label, e.g. "STABLE"
#
# Crontab:
#
#     2 5 * * * /fullpathto/cvs-snapshot.pl -c pathtofolderwithcayenne [-m email@example.com]  2>&1 > /dev/null
#

use strict;
use File::Path;
use File::Copy;
use Getopt::Std;
use Cwd;


our ($opt_c, $opt_m, $opt_l);
getopts('m:c:l:');

die_with_email("Cayenne checkout folder must be defined using -c option") unless $opt_c;
die_with_email("Cayenne checkout folder does not exist: $opt_c") unless -d $opt_c;

my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime();
$year = 1900 + $year;
$mon = 1 + $mon;
$min = "0$min" if $min < 10;
$hour = "0$hour" if $hour < 10;
my $label = "$year-$mon-$mday-$hour$min";	
$label = "$label-$opt_l" if $opt_l;
# my $out_file = "$ENV{'HOME'}/cayenne-cvs-snapshot-$label.txt";
# unlink $out_file if -f $out_file;

# Upload path on the server
my $rel_path = "/var/sites/objectstyle/html/downloads/cayenne/cvs-snapshots";

# print timestamp for log
print_line("\n\n===================================================\n");
print_line("Taking CVS snapshot: $label\n");

# checkout source
get_source("cayenne");
#get_source("sandbox");

# archive
chdir "$opt_c" or die_with_email("Can't change to $opt_c: $!\n");

my $status = run_command("tar --create --gzip --file=$opt_c/cayenne-cvs-snapshot-$label.tar.gz --exclude CVS cayenne");  
die_with_email("Tar failed, return status: $status\n") if $status;

$status = run_command("mv $opt_c/cayenne-cvs-snapshot-$label.tar.gz $rel_path/");
die_with_email("mv failed, return status: $status\n") if $status;

print_line("====== SUCCESS\n");

# --------------------------------------------
# Subs
# --------------------------------------------
sub get_source() {
	my $dir = shift;
	$dir = "$opt_c/$dir";
	die_with_email("Module is not checked out : $dir") unless -d "$dir";
	chdir "$dir" or die_with_email("Can't change to $dir: $!\n");

	my $status = 0;
        my $i = 0;
                 
        # cvs checkouts are unreliable...give it a few tries
	$ENV{"CVS_RSH"} = "ssh";
	for($i = 0; $i < 5; $i++) {
               	$status = run_command(
		" cvs" . 
		" -q" .
		" up" .
		" -dP");
                print_line("CVS checkout status: $status\n");
                last unless $status;
                sleep 120;
         }
	die_with_email("CVS checkout failed, return status: $status, attempts: $i\n") if $status;
}

sub run_command() {
	my $command = shift;
	print_line("# $command\n");
	return system("$command");
}

sub print_line() {
	my $line = shift;
	print $line;
}



sub die_with_email() {
	my $msg = shift;

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


