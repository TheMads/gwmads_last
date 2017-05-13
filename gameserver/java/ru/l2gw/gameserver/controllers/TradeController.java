package ru.l2gw.gameserver.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.NpcTradeList;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.StringTokenizer;

public class TradeController
{
	private static Log _log = LogFactory.getLog(TradeController.class.getName());
	private static TradeController _instance;
	private HashMap<Integer, Integer> _buyPrices;
	private HashMap<Integer, NpcTradeList> _lists;

	public static TradeController getInstance()
	{
		if(_instance == null)
			_instance = new TradeController();
		return _instance;
	}

	public static void reload()
	{
		_instance = new TradeController();
	}

	private TradeController()
	{
		_lists = new HashMap<>();

		try
		{
			File filelists = new File(Config.DATAPACK_ROOT + "/data/merchant_filelists.xml");
			DocumentBuilderFactory factory1 = DocumentBuilderFactory.newInstance();
			factory1.setValidating(false);
			factory1.setIgnoringComments(true);
			Document doc1 = factory1.newDocumentBuilder().parse(filelists);

			int counterFiles = 0;
			int counterItems = 0;
			for(Node n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
				if("list".equalsIgnoreCase(n1.getNodeName()))
					for(Node d1 = n1.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
						if("file".equalsIgnoreCase(d1.getNodeName()))
						{
							final String filename = d1.getAttributes().getNamedItem("name").getNodeValue();

							File file = new File(Config.DATAPACK_ROOT + "/data/" + filename);
							DocumentBuilderFactory factory2 = DocumentBuilderFactory.newInstance();
							factory2.setValidating(false);
							factory2.setIgnoringComments(true);
							Document doc2 = factory2.newDocumentBuilder().parse(file);
							counterFiles++;

							for(Node n2 = doc2.getFirstChild(); n2 != null; n2 = n2.getNextSibling())
								if("list".equalsIgnoreCase(n2.getNodeName()))
									for(Node d2 = n2.getFirstChild(); d2 != null; d2 = d2.getNextSibling())
										if("tradelist".equalsIgnoreCase(d2.getNodeName()))
										{
											final int shop_id = Integer.parseInt(d2.getAttributes().getNamedItem("shop").getNodeValue());
											final int npc_id = Integer.parseInt(d2.getAttributes().getNamedItem("npc").getNodeValue());
											int markup = npc_id > 0 ? d2.getAttributes().getNamedItem("markup") != null ? Integer.parseInt(d2.getAttributes().getNamedItem("markup").getNodeValue()) : 0 : 0;
											NpcTradeList tl = new NpcTradeList(shop_id, npc_id);

											for(Node i = d2.getFirstChild(); i != null; i = i.getNextSibling())
												if("item".equalsIgnoreCase(i.getNodeName()))
												{
													try
													{
														counterItems++;
														final int itemId = Integer.parseInt(i.getAttributes().getNamedItem("id").getNodeValue());
														final int itemCount = i.getAttributes().getNamedItem("count") != null ? Integer.parseInt(i.getAttributes().getNamedItem("count").getNodeValue()) : 0;
														int mup = i.getAttributes().getNamedItem("markup") != null ? Integer.parseInt(i.getAttributes().getNamedItem("markup").getNodeValue()) : markup;
														L2Item item = ItemTable.getInstance().getTemplate(itemId);
														if(item == null)
														{
															_log.warn("TradeController: no item template for item id: " + itemId + " shop id: " + shop_id + " npc id: " + npc_id + " file: " + filename);
															continue;
														}

														tl.addTradeItem(item, mup, itemCount);
													}
													catch(Exception e)
													{
														if(i.getAttributes().getNamedItem("id") != null)
														{
															final int itemId = Integer.parseInt(i.getAttributes().getNamedItem("id").getNodeValue());
															_log.info("TradeController error: filename=" + filename + ", itemId=" + itemId);
														}
														else
															_log.info("TradeController error: filename=" + filename);
														e.printStackTrace();
													}
												}
											_lists.put(shop_id, tl);
										}
						}

			_log.info("TradeController: Loaded " + counterFiles + " file(s).");
			_log.info("TradeController: Loaded " + counterItems + " Items.");
			_log.info("TradeController: Loaded " + _lists.size() + " Buylists.");
		}
		catch(Exception e)
		{
			_log.warn("TradeController: Buylists could not be initialized.");
			e.printStackTrace();
		}

		try
		{
			File classFile = new File(Config.DATAPACK_ROOT, "data/buyprices.csv");
			LineNumberReader lnr = new LineNumberReader(new BufferedReader(new FileReader(classFile)));
			_buyPrices = new HashMap<>();

			// type_name;classid,classid,classid...
			String line;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;

				StringTokenizer st = new StringTokenizer(line, ";");
				if(st.countTokens() < 2)
				{
					_log.info("TradeController: parse error in line " + lnr.getLineNumber() + ": \"" + line + "\"");
					continue;
				}

				while(st.hasMoreTokens())
				{
					try
					{
						_buyPrices.put(Integer.parseInt(st.nextToken()), Integer.parseInt(st.nextToken()));
					}
					catch(Exception e)
					{}
				}
			}

			_log.info("TradeController: Loaded " + _buyPrices.size() + " buy prices.");
		}
		catch(final Exception e)
		{
			_log.warn("TradeController: Error parsing " + Config.DATAPACK_ROOT + "data/buyprices.csv file. " + e);
		}

	}

	public long getBuyPrice(int itemId)
	{
		if(_buyPrices.containsKey(itemId))
			return _buyPrices.get(itemId);

		return -1;
	}

	public NpcTradeList getSellList(int listId)
	{
		return _lists.get(listId);
	}
}