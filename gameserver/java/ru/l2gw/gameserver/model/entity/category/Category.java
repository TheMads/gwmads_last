package ru.l2gw.gameserver.model.entity.category;

import ru.l2gw.commons.arrays.GArray;

import java.util.StringTokenizer;

/**
 * @author rage
 * @date 15.07.11 12:44
 */
public class Category
{
	public final int id;
	public final String name;

	private final GArray<Integer> category = new GArray<Integer>();

	public Category(int id, String name, String values)
	{
		this.id = id;
		this.name = name;
		StringTokenizer st = new StringTokenizer(values);
		while(st.hasMoreTokens())
			category.add(Integer.parseInt(st.nextToken()));
	}

	public boolean isInCategory(int c)
	{
		return category.contains(c);
	}
}
