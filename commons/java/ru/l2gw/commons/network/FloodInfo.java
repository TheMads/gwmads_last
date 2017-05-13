package ru.l2gw.commons.network;

import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author rage
 * @date 16.06.11 15:35
 */
public class FloodInfo
{
	public final String ip;
	public final ConcurrentHashMap<Socket, Long> sockets;

	public FloodInfo(String ip)
	{
		this.ip = ip;
		sockets = new ConcurrentHashMap<>();
	}
}
