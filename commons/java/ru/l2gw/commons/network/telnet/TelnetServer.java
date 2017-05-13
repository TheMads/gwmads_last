package ru.l2gw.commons.network.telnet;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * @author: rage
 * @date: 03.03.12 16:47
 */
public class TelnetServer
{
	private final TelnetServerHandler handler;

	public TelnetServer(String host, int port, String encoding, TelnetServerHandler handler)
	{
		this.handler = handler;
		ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newFixedThreadPool(1), Executors.newFixedThreadPool(1), 1));
		bootstrap.setPipelineFactory(new TelnetPipelineFactory(handler, encoding));
		bootstrap.bind(host.equals("*") ? new InetSocketAddress(port) : new InetSocketAddress(host, port));
	}

	public TelnetServer(String host, int port, TelnetServerHandler handler)
	{
		this(host, port, "UTF-8", handler);
	}

	public void writeToAllConnected(String message)
	{
		handler.writeToAllConnections(message);
	}
}