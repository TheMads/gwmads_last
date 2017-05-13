package ru.l2gw.commons.utils;

import org.w3c.dom.Node;

/**
 * @author: rage
 * @date: 15.04.12 19:00
 */
public class XmlUtil
{
	public static int getIntAttribute(Node node, String name) throws Exception
	{
		try
		{
			return Integer.parseInt(node.getAttributes().getNamedItem(name).getNodeValue());
		}
		catch(NullPointerException e)
		{
			throw new NoSuchFieldException(name);
		}
	}

	public static long getLongAttribute(Node node, String name) throws Exception
	{
		try
		{
			return Long.parseLong(node.getAttributes().getNamedItem(name).getNodeValue());
		}
		catch(NullPointerException e)
		{
			throw new NoSuchFieldException(name);
		}
	}

	public static double getDoubleAttribute(Node node, String name) throws Exception
	{
		try
		{
			return Double.parseDouble(node.getAttributes().getNamedItem(name).getNodeValue());
		}
		catch(NullPointerException e)
		{
			throw new NoSuchFieldException(name);
		}
	}

	public static float getFloatAttribute(Node node, String name) throws Exception
	{
		try
		{
			return Float.parseFloat(node.getAttributes().getNamedItem(name).getNodeValue());
		}
		catch(NullPointerException e)
		{
			throw new NoSuchFieldException(name);
		}
	}

	public static boolean getBooleanAttribute(Node node, String name) throws Exception
	{
		try
		{
			return "1".equals(node.getAttributes().getNamedItem(name).getNodeValue()) || Boolean.parseBoolean(node.getAttributes().getNamedItem(name).getNodeValue());
		}
		catch(NullPointerException e)
		{
			throw new NoSuchFieldException(name);
		}
	}

	public static String getAttribute(Node node, String name) throws Exception
	{
		try
		{
			return node.getAttributes().getNamedItem(name).getNodeValue();
		}
		catch(NullPointerException e)
		{
			throw new NoSuchFieldException(name);
		}
	}

	public static int getIntAttribute(Node node, String name, int def)
	{
		try
		{
			return getIntAttribute(node, name);
		}
		catch(Exception e)
		{
			return def;
		}
	}

	public static long getLongAttribute(Node node, String name, long def)
	{
		try
		{
			return getLongAttribute(node, name);
		}
		catch(Exception e)
		{
			return def;
		}
	}

	public static double getDoubleAttribute(Node node, String name, double def)
	{
		try
		{
			return getDoubleAttribute(node, name);
		}
		catch(Exception e)
		{
			return def;
		}
	}

	public static float getFloatAttribute(Node node, String name, float def)
	{
		try
		{
			return getFloatAttribute(node, name);
		}
		catch(Exception e)
		{
			return def;
		}
	}

	public static boolean getBooleanAttribute(Node node, String name, boolean def)
	{
		try
		{
			return getBooleanAttribute(node, name);
		}
		catch(Exception e)
		{
			return def;
		}
	}

	public static String getAttribute(Node node, String name, String def)
	{
		try
		{
			return getAttribute(node, name);
		}
		catch(Exception e)
		{
			return def;
		}
	}

	public static String getNodeValue(Node node) throws Exception
	{
		Node value = node.getAttributes().getNamedItem("value");
		if(value != null)
			return value.getNodeValue();

		return node.getFirstChild().getNodeValue();
	}

	public static int getIntNodeValue(Node node) throws Exception
	{
		return Integer.parseInt(getNodeValue(node));
	}

	public static long getLongNodeValue(Node node) throws Exception
	{
		return Long.parseLong(getNodeValue(node));
	}

	public static double getDoubleNodeValue(Node node) throws Exception
	{
		return Double.parseDouble(getNodeValue(node));
	}

	public static float getFloatNodeValue(Node node) throws Exception
	{
		return Float.parseFloat(getNodeValue(node));
	}

	public static String getNodeValue(Node node, String def)
	{
		try
		{
			return getNodeValue(node);
		}
		catch(Exception e)
		{
			return def;
		}
	}

	public static int getIntNodeValue(Node node, int def)
	{
		try
		{
			return getIntNodeValue(node);
		}
		catch(Exception e)
		{
			return def;
		}
	}

	public static long getLongNodeValue(Node node, long def)
	{
		try
		{
			return getLongNodeValue(node);
		}
		catch(Exception e)
		{
			return def;
		}
	}

	public static double getDoubleNodeValue(Node node, double def)
	{
		try
		{
			return getDoubleNodeValue(node);
		}
		catch(Exception e)
		{
			return def;
		}
	}
}