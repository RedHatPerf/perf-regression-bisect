# Perf lab GIT auto-bisect

Use binary search to automatically find the commit that introduced a performance regression

The aim of this POC is to demonstrate running an automated bisect to identify the commit causing a performance regression in a range of commits. 

The goal is to be able to;
 - re-run the qDup scripts 
 - use a binary search to quickly identify the commit ID that caused to the performance regression
 - call into Horreum to use the complex regression capabilities of Horreum to validate regressions in a particular commit

## Problem statement

Performance testing takes time to set up and run. Performing end-to-end performance tests on every commit to a project is often not practical.  Therefore batches of changes are tested in defined cycles, i.e. with each release, weekly, daily etc.  

Often when a regression is detected, there is usually a number of commits that have been pushed to a repository since the last performance regression test. It is often the job of a performance engineer to identify the exact commit which caused the regression.  This is a time-consuming task, but with qDup and Horreum we already have most of the automation to hand.

This POC uses qDup, horreum and jGit to automate the manual process of finding the commit that caused a detected performance regression by re-running the exact same tests iterative whilst also performing a binary search over a range of potentially bad commits.

This *should* eliminate a lot of manual testing when a performance regression is detected

## Testing

### Pre-requisite

The target machine should have `sshd` running, on RHEL;

` $ sudo systemctl start sshd`

### Running test-suite

A sample test has been set up that runs the following qDup script:

https://github.com/johnaohara/demo-qDup/blob/main/qDup.yaml

against this hypothetical upstream repository:

https://github.com/johnaohara/demo-repo

The upstream repo does not perform any work, but simple outputs a scalar value.  The scalar value represents a result file from a performance test.

To run the tests;

` $ mvn clean verify`