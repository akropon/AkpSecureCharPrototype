package com.akropon.secureChatPrototype.serverApi;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Discards any incoming data.
 */
public class Server {

    private int port;
    private Channel serverChannel;

    public Server(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        showGui();
        launchServer();
    }

    private void launchServer() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new ServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync(); // (7)

            System.out.println("Server started.");
            System.out.println("port="+port);
            printlnOutPossibleIpAddresses();

            serverChannel = f.channel();

            // Wait until the server socket is closed.
            serverChannel.closeFuture().sync();
        } finally {
            System.out.println("shutdownGracefully... BEGIN");
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println("shutdownGracefully... END");
        }
    }

    private void printlnOutPossibleIpAddresses() {
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress i = (InetAddress) ee.nextElement();
                    System.out.println(i.getHostAddress());
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
            System.err.println(ex);
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        new Server(port).run();
    }


    private void showGui() {
        JFrame gui = createGui();
        SwingUtilities.invokeLater(() -> gui.setVisible(true));
    }

    private JFrame createGui() {
        JFrame viewForm = new JFrame("Main Form");
        viewForm.setSize(200, 100);
        viewForm.setVisible(true);
        viewForm.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        JButton button = new JButton("Click me!");
        button.setVisible(true);
        button.setLocation(12, 12);
        button.setSize(165, 50);
        button.addActionListener(e -> stopServerAndCloseGui(viewForm));
        viewForm.getContentPane().add(button);
        viewForm.getContentPane().add(new JLabel());

        viewForm.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopServerAndCloseGui(viewForm);
            }
        });

        return viewForm;
    }

    private void stopServerAndCloseGui(JFrame gui) {
        System.out.println("Trying to close server channel by closing GUI window");
        if (serverChannel == null) {
            System.out.println("serverChannel in null. Can't close anything.");
        } else {
            System.out.println("Closing server channel by closing GUI window...");
            serverChannel.close();
        }
        System.out.println("Disposing GUI...");
        gui.dispose();
    }
}