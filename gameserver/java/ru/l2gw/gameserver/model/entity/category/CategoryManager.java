package ru.l2gw.gameserver.model.entity.category;

import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2PetInstance;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * @author rage
 * @date 15.07.11 12:42
 */
public class CategoryManager
{
	private static final Log _log = LogFactory.getLog(CategoryManager.class);
	private static final TIntObjectHashMap<Category> categories = new TIntObjectHashMap<>();

	public static boolean isInCategory(int id, int c)
	{
		Category cat = categories.get(id);
		return cat != null && cat.isInCategory(c);
	}

	public static boolean isInCategory(int id, L2Object object)
	{
		if(object == null)
			return false;

		int c = -1;
		if(object.isPlayer())
			c = ((L2Player) object).getActiveClass();
		else if(object.isSummon())
			c = ((L2Summon) object).getNpcId();
		else if(object.isPet())
			c = ((L2PetInstance) object).getNpcId();
		else if(object.isNpc())
			c = ((L2NpcInstance) object).getNpcId();
		else if(object.isItem())
			c = ((L2ItemInstance) object).getItemId();

		if(c < 0)
			return false;

		Category cat = categories.get(id);
		return cat != null && cat.isInCategory(c);
	}

	public static int getCategoryId(String category)
	{
		for(Category cat : categories.valueCollection())
			if(cat.name.equalsIgnoreCase(category))
				return cat.id;

		return -1;
	}

	public static void load()
	{
		try
		{
			_log.info("Category Manager: loading data...");
			File file = new File(Config.CATEGORY_FILE);

			if(!file.exists())
			{
				_log.info("The " + Config.CATEGORY_FILE + " file is missing.");
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(file);

			int id = -1;
			String name = "";
			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
				try
				{
					if("list".equalsIgnoreCase(n.getNodeName()))
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
							if("category".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attrs = d.getAttributes();
								id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
								name = attrs.getNamedItem("name").getNodeValue();
								categories.put(id, new Category(id, name, d.getFirstChild().getNodeValue()));
							}
				}
				catch(Exception e)
				{
					_log.warn("Category Manager: can't load category data for: " + id + " " + name + " " + e);
					e.printStackTrace();
				}

			_log.info("Category Manager: loaded " + categories.size() + " categories.");
		}
		catch(Exception e)
		{
			_log.warn("Category Manager: Error while loading data.");
			e.printStackTrace();
		}
	}
}