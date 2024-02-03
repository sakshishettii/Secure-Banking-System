compile:
	javac BankServer.java
	javac ATM.java

run-bankserver: compile
	java BankServer 8122
run-atm: compile
	java ATM remote05.cs.binghamton.edu 8122
clean:
	rm -r *.class
