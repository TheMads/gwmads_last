package ru.l2gw.gameserver.model;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class Wyvern
{
	public ArrayList<Integer[]> path;
	public String name;

	public void parseLine(String line)
	{
		path = new ArrayList<Integer[]>();
		StringTokenizer st = new StringTokenizer(line, " ");
		name = st.nextToken();
		while(st.hasMoreTokens())
		{
			Integer[] point = null;
			String token = st.nextToken();
			if(token.startsWith("t"))
			{
				StringTokenizer points = new StringTokenizer(token, ";");
				points.nextToken();
				point = new Integer[] {
						Integer.parseInt(points.nextToken()),
						Integer.parseInt(points.nextToken()),
						Integer.parseInt(points.nextToken()),
						-1 };
			}
			else
			{
				StringTokenizer points = new StringTokenizer(token, ";");
				point = new Integer[] { Integer.parseInt(points.nextToken()), Integer.parseInt(points.nextToken()), Integer.parseInt(points.nextToken()) };
			}
			path.add(point);
		}
	}
}
