# /bin/bash
clear
echo "initalizing CS682 project 1.5"
echo "Enter remove server login id:"
RemoteLogin='ayalnajjar'
#read RemoteLogin

# getting data server 1 informaions
echo "Enter Address for data server"
read DatSrvAddr1
echo "Enter Port for data server"
read DatSrvAddrPort2

# getting data server 2 informaions
echo "Enter Address for data server"
read DatSrvAddr2
echo "Enter Port for data server"
read DatSrvAddrPort2

# getting web server 1 informaions
echo "Enter Address for Web server #1:"
read WebSrvAddr1
echo "Enter Port for Web server #1:"
read WebSrvAddrPort1

# getting web server 2 informaions
echo "Enter Address for Web server #2:"
read WebSrvAddr2
echo "Enter Port for Web server #1:"
read WebSrvAddrPort2


ssh "$RemoteLogin@$DatSrvAddr1" "java -jar twitter-light.jar data $DatSrvAddr1 $DatSrvAddrPort1 &"
ssh "$RemoteLogin@$DatSrvAddr1" "java -jar twitter-light.jar data $DatSrvAddr2 $DatSrvAddrPort2 $DatSrvAddr1 $DatSrvAddrPort1&"
ssh "$RemoteLogin@$WebSrvAddr1" "java -jar twitter-light.jar web $WebSrvAddrPort1 $DatSrvAddr1 $DatSrvAddrPort1 &"
ssh "$RemoteLogin@$WebSrvAddr2" "java -jar twitter-light.jar web $WebSrvAddrPort2 $DatSrvAddr2 $DatSrvAddrPort2 $DatSrvAddr1 $DatSrvAddrPort1 &"

echo "Please use option 1 to setup your client to access one of the folowing web servers:"
echo "Web Server #1 address:$WebSrvAddr1 Port:$WebSrvAddrPort1"
echo "Web Server #1 address:$WebSrvAddr2 Port:$WebSrvAddrPort2"