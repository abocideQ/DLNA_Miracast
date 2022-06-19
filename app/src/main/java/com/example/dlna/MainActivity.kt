package com.example.dlna

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.MulticastSocket

class MainActivity : AppCompatActivity() {

    private lateinit var mContentView: LinearLayout

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mContentView = findViewById(R.id.ll_content)
        val buttonSearch = Button(this)
        buttonSearch.text = "搜索"
        buttonSearch.setOnClickListener {
            Thread {//DLNA
                val content = "M-SEARCH * HTTP/1.1\r\n" +
                        "HOST: 239.255.255.250:1900\r\n" +
                        "MX: 5\r\n" +
                        "MAN: ssdp:discover\r\n" +
                        "ST: ssdp:all\r\n"
                udpSend("239.255.255.250", 1900, content)
            }.start()
        }
        val buttonClean = Button(this)
        buttonClean.text = "清空"
        buttonClean.setOnClickListener {
            mContentView.removeAllViews()
            mContentView.addView(buttonClean)
            mContentView.addView(buttonSearch)
        }
        mContentView.addView(buttonClean)
        mContentView.addView(buttonSearch)
        Thread { udpReceive(null, 8080) }.start()
        Thread { udpSend("255.255.255.255", 8080, "udp all Broadcast test test test\n") }.start()
        Thread { udpReceive("239.255.255.250", 1900) }.start()
        Thread { udpSend("239.255.255.250", 1900, "udp group Broadcast test test test\n") }.start()
    }

    private fun udpSend(ip: String, port: Int, content: String) {
        pushMessageView(mContentView, "^^^ udp send $ip:$port\n $content")
        val socket = DatagramSocket()
        socket.send(
            DatagramPacket(
                content.toByteArray(),
                content.toByteArray().size,
                InetAddress.getByName(ip),
                port
            )
        )
        socket.close()
    }

    private fun udpReceive(ip: String?, port: Int) {
        val socket: DatagramSocket = if (ip.isNullOrEmpty()) {
            //单播 广播
            val socket = DatagramSocket(port)
            socket
        } else {
            //组播
            val socket = MulticastSocket(port)
            socket.joinGroup(InetAddress.getByName(ip))
            socket
        }
        while (true) {
            val packet = DatagramPacket(ByteArray(1024 * 10), 1024 * 10)
            socket.receive(packet)
            pushMessageView(
                mContentView,
                "### udp receive $port\n ${String(packet.data, packet.offset, packet.length)}"
            )
        }
    }

    @SuppressLint("WifiManagerLeak")
    private fun getIp(): String {
        val wifiManager: WifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo;
        val ipAddress = wifiInfo.ipAddress
        return ((ipAddress and 0xff).toString() + "." + (ipAddress shr 8 and 0xff) + "." + (ipAddress shr 16 and 0xff) + "." + (ipAddress shr 24 and 0xff))
    }

    private fun pushMessageView(parent: LinearLayout, content: String) {
        val textView = TextView(parent.context)
        textView.setTextColor(ContextCompat.getColor(parent.context, R.color.black))
        textView.text = content
        runOnUiThread { parent.addView(textView) }
    }

    /**
     * 主动通知方式
     *
     * ssdp:alive 消息 添加到网络
     * NOTIFY * HTTP/1.1           // 消息头
     * NT:                         // 在此消息中，NT头必须为服务的服务类型。（如：upnp:rootdevice）
     * HOST:                       // 设置为协议保留多播地址和端口，必须是：239.255.255.250:1900（IPv4）或FF0x::C(IPv6
     * NTS:                        // 表示通知消息的子类型，必须为ssdp:alive
     * LOCATION:                   // 包含根设备描述得URL地址  device 的webservice路径（如：http://127.0.0.1:2351/1.xml)
     * CACHE-CONTROL:              // max-age指定通知消息存活时间，如果超过此时间间隔，控制点可以认为设备不存在 （如：max-age=1800）
     * SERVER:                     // 包含操作系统名，版本，产品名和产品版本信息( 如：Windows NT/5.0, UPnP/1.0)
     *  USN:                        // 表示不同服务的统一服务名，它提供了一种标识出相同类型服务的能力。如：
     * // 根/启动设备 uuid:f7001351-cf4f-4edd-b3df-4b04792d0e8a::upnp:rootdevice
     * // 连接管理器  uuid:f7001351-cf4f-4edd-b3df-4b04792d0e8a::urn:schemas-upnp-org:service:ConnectionManager:1
     * // 内容管理器 uuid:f7001351-cf4f-4edd-b3df-4b04792d0e8a::urn:schemas-upnp-org:service:ContentDirectory:1
     *
     * ssdp:byebye 消息 网络中退出
     * NOTIFY * HTTP/1.1       // 消息头
     * HOST:                   // 设置为协议保留多播地址和端口，必须是：239.255.255.250:1900（IPv4）或FF0x::C(IPv6
     * NTS:                    // 表示通知消息的子类型，必须为ssdp:byebye
     * USN:                    // 同上
     * */
    /**
     * 搜索
     *
     * 多播搜索消息
     * M-SEARCH * HTTP/1.1             // 请求头 不可改变
     * HOST: 239.255.255.250:1900      // 设置为协议保留多播地址和端口，必须是：239.255.255.250:1900（IPv4）或FF0x::C(IPv6
     * MX: 5                           // 设置设备响应最长等待时间，设备响应在0和这个值之间随机选择响应延迟的值。这样可以为控制点响应平衡网络负载。
     * MAN: "ssdp:discover"            // 设置协议查询的类型，必须是：ssdp:discover
     * // ALIVE("ssdp:alive"),
     * // UPDATE("ssdp:update"),
     * // BYEBYE("ssdp:byebye"),
     * // ALL("ssdp:all"),
     * // DISCOVER("ssdp:discover"),
     * // PROPCHANGE("upnp:propchange");
     * ST: upnp:rootdevice             // 设置服务查询的目标，它必须是下面的类型：
     * // ssdp:all  搜索所有设备和服务
     * // upnp:rootdevice  仅搜索网络中的根设备
     * // uuid:device-UUID  查询UUID标识的设备
     * // urn:schemas-upnp-org:device:device-Type:version  查询device-Type字段指定的设备类型，设备类型和版本由UPNP组织定义。
     * // urn:schemas-upnp-org:service:service-Type:version  查询service-Type字段指定的服务类型，服务类型和版本由UPNP组织定义。
     *
     * 多播搜索响应  主要关注带有 * 的部分即可
     * HTTP/1.1 200 OK             // * 消息头
     * LOCATION:                   // * 包含根设备描述得URL地址  device 的webservice路径（如：http://127.0.0.1:2351/1.xml)
     * CACHE-CONTROL:              // * max-age指定通知消息存活时间，如果超过此时间间隔，控制点可以认为设备不存在 （如：max-age=1800）
     * SERVER:                     // 包含操作系统名，版本，产品名和产品版本信息( 如：Windows NT/5.0, UPnP/1.0)
     * EXT:                        // 为了符合HTTP协议要求，并未使用。
     * BOOTID.UPNP.ORG:            // 可以不存在，初始值为时间戳，每当设备重启并加入到网络时+1，用于判断设备是否重启。也可以用于区分多宿主设备。
     * CONFIGID.UPNP.ORG:          // 可以不存在，由两部分组成的非负十六进制整数，由两部分组成，第一部分代表跟设备和其上的嵌入式设备，第二部分代表这些设备上的服务。
     * USN:                        // * 表示不同服务的统一服务名
     * ST:                         // * 服务的服务类型
     * DATE:                       // 响应生成时间
     * */
}