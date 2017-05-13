package ru.l2gw.fakeserver.manager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.fakeserver.Config;
import ru.l2gw.fakeserver.client.ServerClient;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: rage
 * @date: 18.04.13 16:44
 */
public class ServerManager implements Runnable
{
	protected static Log log = LogFactory.getLog(ServerManager.class);

	private int max, online1, online2, store;
	private Map<ServerInfo, ServerClient> clients;

	public ServerManager()
	{
		clients = new HashMap<>();
		for(ServerInfo info : Config.SERVERS)
			if(!clients.containsKey(info))
				clients.put(info, new ServerClient(info.host, info.port));
	}

	@Override
	public void run()
	{
		log.info("Start request online task.");
		int m = 0, o1 = 0, o2 = 0, s = 0;
		for(ServerClient client : clients.values())
		{
			if(client.readOnline())
			{
				m += client.getMax();
				o1 += client.getOnline1();
				o2 += client.getOnline2();
				s += client.getStore();
				log.info("Read online from: " + client);
			}
			else
			{
				log.info("Can't read online: " + client);
			}
		}

		max = m;
		online1 = o1;
		online2 = o2;
		store = s;
	}

	public int getMax()
	{
		return max;
	}

	public void setMax(int max)
	{
		this.max = max;
	}

	public int getOnline1()
	{
		return online1;
	}

	public void setOnline1(int online1)
	{
		this.online1 = online1;
	}

	public int getOnline2()
	{
		return online2;
	}

	public void setOnline2(int online2)
	{
		this.online2 = online2;
	}

	public int getStore()
	{
		return store;
	}

	public void setStore(int store)
	{
		this.store = store;
	}

	public Map<ServerInfo, ServerClient> getClients()
	{
		return clients;
	}
}
