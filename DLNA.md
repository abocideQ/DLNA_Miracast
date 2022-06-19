#### socket udp
1. 发送udp
byte data[] = "test test test udp".getBytes();
DatagramPacket packet = new DatagramPacket(data, data.length() ,InetAddress.getByName("192.168.50.3"), 8080);
DatagramSocket().send(packet);

2. 接收udp
DatagramSocket socket = new DatagramSocket(8080);
while (true){
    DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
    socket.receive(packet);
    System.out.println("udp: " + new String(packet.getData(), packet.getOffset(), packet.getLength()));
}


#### socket broadcast
单播: 指定ip通信, 可广域网
组播: 组播ip 通信, 可广域网, 224.0.0.0 ~ 224.0.0.255为预留的组播地址, 地址224.0.0.0保留不做分配, 其它地址供路由协议使用
广播：广播ip 通信, 仅局域网, 255.255.255.255 为全网段广播地址, 如 192.168.1.255 为192.168.1.x网关广播
受限广播(需要绑定具体网卡): 255.255.255.255 所有网段可接收, 但路由器不转发到广域网
直接广播: 192.168.1.255 当前网关可接收


#### host ip
iAddress = InetAddress.getLocalHost();
System.out.println("host:" + iAddress.getHostName());
System.out.println("ip:" + iAddress.getHostAddress());
example: 192.168.1.50


#### local network
1. ping 
for (int i = 0; i < 255 ; i++){
    ping("192.168.1" + i);
}