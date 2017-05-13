package ru.l2gw.gameserver.tables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.base.L2Augmentation;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.variation.Variation;
import ru.l2gw.gameserver.model.variation.VariationFee;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author: rage
 * @date: 18.10.11 21:40
 */
public class VariationData
{
	private static final Log _log = LogFactory.getLog("variation");

	private static final GArray<Variation> variations = new GArray<>();
	private static final GArray<Integer> variation_exception = new GArray<>();
	private static final HashMap<String, GArray<Integer>> itemGroups = new HashMap<>();
	private static final HashMap<String, HashMap<Integer, VariationFee>> variationFee = new HashMap<>();
	private static final HashMap<Integer, Integer> mineralLevel = new HashMap<>();

	public static boolean isValidItem(int itemId)
	{
		return !variation_exception.contains(itemId) && getItemGroup(itemId) != null;
	}

	public static String getItemGroup(int itemId)
	{
		for(Map.Entry<String, GArray<Integer>> entry : itemGroups.entrySet())
			if(entry.getValue().contains(itemId))
				return entry.getKey();

		return null;
	}

	public static VariationFee getVariationFee(int itemId, int mineralId)
	{
		String groupName = getItemGroup(itemId);
		if(groupName == null)
			return null;

		if(!variationFee.containsKey(groupName))
			return null;

		if(mineralId == 0)
			return variationFee.get(groupName).values().iterator().next();

		return variationFee.get(groupName).get(mineralId);
	}

	public static int getMineralLevel(int mineralId)
	{
		if(!mineralLevel.containsKey(mineralId))
			return 99;

		return mineralLevel.get(mineralId);
	}

	public static L2Augmentation generateRandomVariation(L2ItemInstance targetItem, int mineralId)
	{
		Variation variation = null;

		for(Variation var : variations)
		{
			if(var.getMineralId() == mineralId && targetItem.getItem().isMagicWeapon() == var.isMagicWeapon())
			{
				variation = var;
				break;
			}
		}

		if(variation == null)
		{
			_log.warn("VariationData: no variation for: " + targetItem + " mineralId: " + mineralId);
			return null;
		}

		int effects = variation.getRandomVariation();
		if(effects == 0)
		{
			_log.warn("VariationData: effects 0 for: " + targetItem + " mineral: " + mineralId);
			return null;
		}

		return new L2Augmentation(targetItem, effects, mineralId, true);
	}

	public static void load()
	{
		try
		{
			File file = new File(Config.VARIATIONDATA_FILE);

			if(!file.exists())
			{
				_log.info("VariationData: " + Config.VARIATIONDATA_FILE + " file is missing.");
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(file);
			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				try
				{
					if("variationdata".equalsIgnoreCase(n.getNodeName()))
					{
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if("variation".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attr = d.getAttributes();
								boolean isMagic = attr.getNamedItem("weapon_type").getNodeValue().equalsIgnoreCase("mage");
								int mineral = Integer.parseInt(attr.getNamedItem("mineral").getNodeValue());

								Variation variation = new Variation(mineral, isMagic);

								for(Node v = d.getFirstChild(); v != null; v = v.getNextSibling())
								{
									if("variation1".equals(v.getNodeName()))
									{
										variation.setVariation1(Variation.parseGroup(v.getFirstChild().getNodeValue()));
									}
									else if("variation2".equals(v.getNodeName()))
									{
										variation.setVariation2(Variation.parseGroup(v.getFirstChild().getNodeValue()));
										break;
									}
								}

								variations.add(variation);
							}
							else if("itemgroup".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attr = d.getAttributes();
								String name = attr.getNamedItem("name").getNodeValue();
								StringTokenizer st = new StringTokenizer(d.getFirstChild().getNodeValue());
								GArray<Integer> items = new GArray<>(st.countTokens());
								while(st.hasMoreTokens())
									items.add(Integer.parseInt(st.nextToken()));

								itemGroups.put(name, items);
							}
							else if("fee".equalsIgnoreCase(d.getNodeName()))
							{
								NamedNodeMap attr = d.getAttributes();
								String itemGroup = attr.getNamedItem("item_group").getNodeValue();

								if(!itemGroups.containsKey(itemGroup))
								{
									_log.warn("VariationData: warning now item group: " + itemGroup + " for fee.");
									continue;
								}

								int mineral = Integer.parseInt(attr.getNamedItem("mineral").getNodeValue());
								int fee_item = Integer.parseInt(attr.getNamedItem("fee_item").getNodeValue());
								long fee_count = Long.parseLong(attr.getNamedItem("fee_count").getNodeValue());
								long cancel_fee = Long.parseLong(attr.getNamedItem("cancel_fee").getNodeValue());
								HashMap<Integer, VariationFee> fee = variationFee.get(itemGroup);
								if(fee == null)
								{
									fee = new HashMap<>();
									variationFee.put(itemGroup, fee);
								}

								fee.put(mineral, new VariationFee(fee_item, fee_count, cancel_fee));
							}
							else if("mineral".equalsIgnoreCase(d.getNodeName()))
							{
								int level = Integer.parseInt(d.getAttributes().getNamedItem("level").getNodeValue());
								StringTokenizer st = new StringTokenizer(d.getFirstChild().getNodeValue());
								while(st.hasMoreTokens())
									mineralLevel.put(Integer.parseInt(st.nextToken()), level);
							}
							else if("variation_exception".equalsIgnoreCase(d.getNodeName()))
							{
								StringTokenizer st = new StringTokenizer(d.getFirstChild().getNodeValue());
								while(st.hasMoreTokens())
									variation_exception.add(Integer.parseInt(st.nextToken()));
							}
						}
					}
				}
				catch(Exception e)
				{
					_log.warn("VariationData: can't load product data" + e);
					e.printStackTrace();
				}
			}

			_log.info("VariationData: loaded " + variations.size() + " variations.");
			_log.info("VariationData: loaded " + itemGroups.size() + " item groups.");
		}
		catch(Exception e)
		{
			_log.warn("VariationData: error while product data" + e);
			e.printStackTrace();
		}
	}
}