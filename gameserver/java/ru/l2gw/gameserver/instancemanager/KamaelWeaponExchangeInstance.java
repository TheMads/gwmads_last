package ru.l2gw.gameserver.instancemanager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2KamaelWeaponExchange;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class KamaelWeaponExchangeInstance
{
	private static Log _log = LogFactory.getLog(KamaelWeaponExchangeInstance.class.getName());
	private static KamaelWeaponExchangeInstance _instance;

	static Map<Integer, L2KamaelWeaponExchange> _items;

	public static KamaelWeaponExchangeInstance getInstance()
	{
		if(_instance == null)
			_instance = new KamaelWeaponExchangeInstance();
		return _instance;
	}

	public static void reload()
	{
		_instance = new KamaelWeaponExchangeInstance();
	}

	public KamaelWeaponExchangeInstance()
	{
		_items = new HashMap<Integer, L2KamaelWeaponExchange>();
		_log.info("KamaelWeaponExchange: Initializing");
		load();
		_log.info("KamaelWeaponExchange: Loaded " + _items.size() + " items.");
	}

	private void load()
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			File file = new File(Config.DATAPACK_ROOT + "/data/kamael_weapon_exchange.xml");
			if(!file.exists())
			{
				if(Config.DEBUG)
					System.out.println("KamaelWeaponExchange: NO FILE");
				return;
			}
			int counter = 0;
			Document doc = factory.newDocumentBuilder().parse(file);
			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				if("list".equalsIgnoreCase(n.getNodeName()))
					for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						if("item".equalsIgnoreCase(d.getNodeName()))
						{
							NamedNodeMap attrs = d.getAttributes();
							int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
							if(id == 0)
								continue;
							int kamael_id = Integer.parseInt(attrs.getNamedItem("kamael_id").getNodeValue());
							if(kamael_id == 0)
								continue;
							L2KamaelWeaponExchange kwe = new L2KamaelWeaponExchange();
							kwe.setOriginal(id);
							kwe.setKamael(kamael_id);
							_items.put(counter, kwe);
							counter++;
						}
			if(Config.DEBUG)
				System.out.println("KamaelWeaponExchange: OK");
		}
		catch(Exception e)
		{
			_log.error("KamaelWeaponExchange: Error parsing kamael_weapon_exchange.xml. " + e);
		}
	}

	public static int convertWeaponId(int itemId)
	{
		for(int i = 0; i < _items.size(); i++)
		{
			if(itemId == _items.get(i).getKamael())
				return _items.get(i).getOriginal();
			else if(itemId == _items.get(i).getOriginal())
				return _items.get(i).getKamael();
		}
		return 0;
	}

}