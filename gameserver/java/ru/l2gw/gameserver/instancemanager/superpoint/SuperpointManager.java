package ru.l2gw.gameserver.instancemanager.superpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;

/**
 * @author rage
 * @date 25.11.2010 13:55:13
 */
public class SuperpointManager
{
	private static final Log _log = LogFactory.getLog(SuperpointManager.class);
	private static SuperpointManager _instance;
	private static HashMap<String, Superpoint> _superpoints;

	private SuperpointManager()
	{
		load();
	}

	public static SuperpointManager getInstance()
	{
		if(_instance == null)
			_instance = new SuperpointManager();

		return _instance;
	}

	private void load()
	{
		_log.info(this + " Initializing...");
		_superpoints = new HashMap<String, Superpoint>();

		try
		{
			File file = new File(Config.SUPERPOINT_FILE);

			if(!file.exists())
			{
				if(Config.DEBUG)
					_log.info("The " + Config.SUPERPOINT_FILE + " file is missing.");
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(file);

			for(Node l = doc.getFirstChild(); l != null; l = l.getNextSibling())
				try
				{
					if("list".equalsIgnoreCase(l.getNodeName()))
						for(Node s = l.getFirstChild(); s != null; s = s.getNextSibling())
							if("superpoint".equalsIgnoreCase(s.getNodeName()))
							{
								String name = s.getAttributes().getNamedItem("name").getNodeValue();
								String type = s.getAttributes().getNamedItem("type").getNodeValue();
								Superpoint sp = new Superpoint(name, type);

								for(Node n = s.getFirstChild(); n != null; n = n.getNextSibling())
								{
									if("node".equalsIgnoreCase(n.getNodeName()))
									{
										NamedNodeMap attr = n.getAttributes();
										int id = Integer.parseInt(attr.getNamedItem("id").getNodeValue());
										int x = Integer.parseInt(attr.getNamedItem("x").getNodeValue());
										int y = Integer.parseInt(attr.getNamedItem("y").getNodeValue());
										int z = Integer.parseInt(attr.getNamedItem("z").getNodeValue());
										int message = attr.getNamedItem("fStringId") != null ? Integer.parseInt(attr.getNamedItem("fStringId").getNodeValue()) : -1;
										int social = attr.getNamedItem("social") != null ? Integer.parseInt(attr.getNamedItem("social").getNodeValue()) : -1;
										int delay = attr.getNamedItem("delay") != null ? Integer.parseInt(attr.getNamedItem("delay").getNodeValue()) : -1;

										sp.addNode(x, y, z, message, social, delay, id);
									}
								}

								_superpoints.put(name, sp);
							}
				}
				catch(Exception	e)
				{
					_log.warn(this + " can't load superpoint data" + e);
					e.printStackTrace();
				}

			_log.info(this + " loaded: " + _superpoints.size() + " superpoints.");
		}
		catch(Exception e)
		{
			_log.warn(this + " can't load superpoint data: " + e);
			e.printStackTrace();
		}
	}

	public Superpoint getSuperpointByName(String name)
	{
		if(name.contains("["))
			name = name.replace("[", "").replace("]", "");

		return _superpoints.get(name);
	}

	@Override
	public String toString()
	{
		return "SuperpointManager:";
	}
}
