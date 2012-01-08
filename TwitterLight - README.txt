
USAGE:
(1) Using command line:
	(A) start a data server:
		java -jar twitter-light.jar dataserver <DataServer Addree> <DataServer Port> <Entrty Point address(O)> <Entry point port(o)>
			Parameters:
			<DataServer Addree>: the adderss of the node where you want to run it.
			<DataServer Port>: the port of the node to listen to.
			<Entrty Point address(O)>: (optional) internet address for a replica.
			<Entry point port(o)>: (optional) node port for a replica.
	
	(b) start a web server:
		java -jar twitter-light.jar webserver <WebServer Address> <WebServer Port> <DataServer Address> <DataServer Port> <Entrty Point address(o)> <Entry point port(o)>
			Parameters:
			<WebServer Address>: the adderss of the node where you want to run it.
			<WebServer Port>: the port of the node to listen to.
			<DataServer Addree>: data server adderss.
			<DataServer Port>: data server port.
			<Entrty Point address(O)>: (optional) internet address for a replica.
			<Entry point port(o)>: (optional) node port for a replica.

(2) using script:
	the setup.sh script will guide you to fire a system thats include 2 data servers and 2 web servers to run it:
		bash setup.sh