# TCP Statistical Ping

TCP Statistical Ping is a simple utility with traditional ping functionalities that are
enriched with additional statistical information like:
- number of packets per second that were successfully sent from host A to host B and back. The size of packets and packet generation rate can be configured.
- average round trip times, host A to host B and host B to host A times calculated every second.

The utility consists of a Pitcher who generates the packets, and a Catcher who receives the packets and sends them back to the Pitcher.  

## Building from source
**Prerequisites:**  
- Java 1.7.0+
- Git
- Maven

**Building:**  
1. Clone the git repository  
`git clone https://github.com/ico77/tcp-ping.git`  
2. Change to the repository root directory  
`cd tcp-ping`  
3. Run the Maven package build phase  
`mvn package`  

## Starting the utility
1. Unzip the tcp-ping-<version>-distribution.zip file to a folder of your choice.
2. Use the supplied scripts to start the utility.
	* On Windows:
		- Run pitcher.cmd to start the pitcher
		- Run catcher.cmd to start the catcher
	* On Linux (make sure the scripts are executable):
		- Run pitcher.sh to start the pitcher
		- Run catcher.sh to start the catcher
3. (Optional) Edit the scripts to change options like packet generation rate, packet size,...

