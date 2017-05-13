package ru.l2gw.fakeserver;

import ru.l2gw.fakeserver.client.ServerClient;

/**
 * @author: rage
 * @date: 18.04.13 16:21
 */
public class TestClient
{
	public static void main(String[] args) throws Exception
	{

		ServerClient client = new ServerClient("localhost", 7777);
		if(client.readOnline())
		{
			System.out.println("max: " + client.getMax() + " online: " + client.getOnline1() + "/" + client.getOnline2() + "/" + client.getStore());
		}
		else
		{
			System.out.println("error");
		}
	}
}
