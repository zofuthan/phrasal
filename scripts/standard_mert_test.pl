#!/usr/bin/perl

use File::Compare;

$start_time = time;

$date_tag=`date +%Y-%m-%d`; chomp $date_tag;
$proc_tag=$ARGV[0];
shift @ARGV;

`rm -rf /u/nlp/data/mt_test/mert/phrasal-mert.$proc_tag`;
`(cd  /u/nlp/data/mt_test/mert/; $ENV{"JAVANLP_HOME"}/projects/mt/scripts/phrasal-mert.pl --nbest=500 --working-dir=phrasal-mert.$proc_tag dev2006.fr.lowercase.h10 dev2006.en.lowercase.h10 bleu base.ini > phrasal-mert.$proc_tag.$date_tag.log 2>&1)`;
#`touch phrasal-mert.$proc_tag.$date_tag.log`;
$total_time = time - $start_time;
if (compare("/u/nlp/data/mt_test/mert/phrasal-mert.$proc_tag/phrasal.10.trans", "/u/nlp/data/mt_test/mert/expected-pmert-dir/phrasal.10.trans") == 0) {
	print "Test Success (Time: $total_time s).\n";
  $exitStatus = 0;
} else {
	print "Test Failure (Time: $total_time s).\n";
  $exitStatus = -1;
}

$log = `cat phrasal-mert.$proc_tag.$date_tag.log`;
$from_addr = "javanlp-mt-no-reply\@mailman.stanford.edu";
foreach $emailAddr (@ARGV) {
  if ($exitStatus == 0) {
     $subject = "MT daily integration test ($data_tag) was successful!";
     $body    = "Hello $emailAddr,\n\n".
                "The $data_tag MT daily integration test was sucessful!\n\n";
  } else {
     $subject = "MT daily integration test ($date_tag) FAILED!";
     $body = "Hello $emailAddr,\n\n".
            "The $data_tag MT daily integration test FAILED!\n\n";
  }
  $body .= "Log File:\n\n$log\n";
  # print "| mail -s \"$subject\" $emailAddr -- -f $from_addr < $body";
  open(fh, "| mail -s \"$subject\" $emailAddr -- -f $from_addr");
  print fh $body;
  close(fh); 
}

exit $exitStatus;
