package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2ExtractableItems;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ExtractableItems
{
	private static Log _log = LogFactory.getLog(ExtractableItems.class.getName());
	private static ExtractableItems _instance;

	static Map<Integer, L2ExtractableItems> _lists;

	public static ExtractableItems getInstance()
	{
		if(_instance == null)
			_instance = new ExtractableItems();
		return _instance;
	}

	public static void reload()
	{
		_instance = new ExtractableItems();
	}

	public ExtractableItems()
	{
		_lists = new HashMap<>();
		_log.info("ExtractableItems: Initializing");
		load();
		_log.info("ExtractableItems: Loaded " + _lists.size() + " Extractable.");
	}

	private void load()
	{
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			final File file = new File(Config.DATAPACK_ROOT + "/data/extractable_items.xml");
			if(!file.exists())
			{
				if(Config.DEBUG)
					System.out.println("ExtractableItems: NO FILE");
				return;
			}

			final Document doc = factory.newDocumentBuilder().parse(file);
			int counterItems = 0;

			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				if("list".equalsIgnoreCase(n.getNodeName()))
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						if("item".equalsIgnoreCase(d.getNodeName()))
						{
							final NamedNodeMap attrs = d.getAttributes();
							final int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							if(id == 0)
								continue;
							counterItems++;

							final L2ExtractableItems ei = new L2ExtractableItems(id);
							for(Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
								if("product".equalsIgnoreCase(cd.getNodeName()))
								{
									final int itemId = Integer.parseInt(cd.getAttributes().getNamedItem("id").getNodeValue());
									final int min = Integer.parseInt(cd.getAttributes().getNamedItem("min").getNodeValue());
									final int max = Integer.parseInt(cd.getAttributes().getNamedItem("max").getNodeValue());
									final double chance = Double.parseDouble(cd.getAttributes().getNamedItem("chance").getNodeValue());
									ei.addProduct(itemId, min, max, chance);
								}

							if(_lists.containsKey(id))
								_log.info("ExtractableItems: bad item " + id + " is duplicate");
							else
								_lists.put(id, ei);
						}
		}
		catch(final Exception e)
		{
			_log.error("ExtractableItems: Error parsing extractable_items file. " + e);
		}
	}

	public static boolean useHandler(final L2Playable playable, final L2ItemInstance item)
	{
		if(playable == null || item == null)
			return false;

		if(!playable.isPlayer())
			return false;

		final L2ExtractableItems ei = _lists.get(item.getItemId());
		if(ei == null)
			return false;

		L2Player player = playable.getPlayer();
		if(player.getInventory().slotsLeft() < 10)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOUR_INVENTORY_IS_FULL));
			player.sendActionFailed();
			return false;
		}

		return ei.extractItem(item, player);
	}
}