package ru.l2gw.gameserver.model;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.base.MultiSellEntry;
import ru.l2gw.gameserver.model.base.MultiSellHandler;
import ru.l2gw.gameserver.model.base.MultiSellIngredient;
import ru.l2gw.gameserver.model.base.MultiSellListContainer;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.MultiSellList;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2EtcItem;
import ru.l2gw.gameserver.templates.L2Item;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashSet;
import java.util.List;

/**
 * Multisell list manager
 */
public class L2Multisell
{
	private static Log _log = LogFactory.getLog(L2Multisell.class.getName());
	private FastMap<Integer, MultiSellListContainer> entries = new FastMap<Integer, MultiSellListContainer>();
	private static L2Multisell _instance = new L2Multisell();
	private static FastMap<Integer, MultiSellHandler> _handlers = new FastMap<Integer, MultiSellHandler>();

	public MultiSellListContainer getList(int id)
	{
		return entries.get(id);
	}

	public L2Multisell()
	{
		parseData();
	}

	public void reload()
	{
		parseData();
	}

	public static L2Multisell getInstance()
	{
		return _instance;
	}

	private void parseData()
	{
		entries.clear();
		parse();
	}


	private void hashFiles(String dirname, List<File> hash)
	{
		File dir = new File(Config.DATAPACK_ROOT, "data/" + dirname);
		if(!dir.exists())
		{
			_log.warn("Dir " + dir.getAbsolutePath() + " not exists");
			return;
		}
		File[] files = dir.listFiles();
		for(File f : files)
			if(f.getName().endsWith(".xml"))
				hash.add(f);
			else if(f.isDirectory() && !f.getName().equals(".svn"))
				hashFiles("multisell/" + f.getName(), hash);
	}

	private void parse()
	{
		Document doc = null;
		int id = 0;
		List<File> files = new FastList<File>();
		hashFiles("multisell", files);

		for(File f : files)
		{
			try
			{
				id = Integer.parseInt(f.getName().replaceAll(".xml", ""));
			}
			catch(Exception e)
			{
				_log.warn("Error loading file " + f, e);
				continue;
			}
			try
			{
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setValidating(false);
				factory.setIgnoringComments(true);
				doc = factory.newDocumentBuilder().parse(f);
			}
			catch(Exception e)
			{
				_log.warn("Error loading file " + f, e);
			}
			try
			{
				MultiSellListContainer list = parseDocument(doc);
				list.setListId(id);
				if(list.getKeepEnchant() && list.getShowAll())
				{
					_log.info("Warning: multisell list: " + list.getListId() + " has wrong config! keep enchant and show all is true.");
					list.setShowAll(false);
				}
				entries.put(id, list);
			}
			catch(Exception e)
			{
				_log.warn("Error in file " + f, e);
			}
		}
	}

	protected MultiSellListContainer parseDocument(Document doc)
	{
		MultiSellListContainer list = new MultiSellListContainer();
		int entId = 1;

		for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			if("list".equalsIgnoreCase(n.getNodeName()))
				for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					if("item".equalsIgnoreCase(d.getNodeName()))
					{
						MultiSellEntry e = parseEntry(d);
						e.setEntryId(entId++);
						list.addEntry(e);
					}
					else if("config".equalsIgnoreCase(d.getNodeName()))
					{
						list.setShowAll(Boolean.parseBoolean(getSubNode(d, "showall")));
						list.setNoTax(Boolean.parseBoolean(getSubNode(d, "notax")));
						list.setKeepEnchant(Boolean.parseBoolean(getSubNode(d, "keepenchanted")));
						list.nokey = Boolean.parseBoolean(getSubNode(d, "nokey"));
						list.community = Boolean.parseBoolean(getSubNode(d, "community"));
					}

		return list;
	}

	private String getSubNode(Node n, String item)
	{
		try
		{
			return n.getAttributes().getNamedItem(item).getNodeValue();
		}
		catch(NullPointerException e)
		{
			return null;
		}
	}

	protected MultiSellEntry parseEntry(Node n)
	{
		Node first = n.getFirstChild();
		MultiSellEntry entry = new MultiSellEntry();

		for(n = first; n != null; n = n.getNextSibling())
			if("ingredient".equalsIgnoreCase(n.getNodeName()))
			{
				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());

				entry.addIngredient(new MultiSellIngredient(id, count));
			}
			else if("production".equalsIgnoreCase(n.getNodeName()))
			{
				int id = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
				int count = Integer.parseInt(n.getAttributes().getNamedItem("count").getNodeValue());

				entry.addProduct(new MultiSellIngredient(id, count));
			}

		return entry;
	}

	public void SeparateAndSend(int listId, L2Player player, double taxRate)
	{
		MultiSellListContainer list = generateMultiSell(listId, player, taxRate);
		if(list == null)
		{
			_log.info("L2Multisell: list=null, listId=" + listId + " " + player + " taxRate=" + taxRate);
			return;
		}

		int page = 1;

		MultiSellListContainer temp = new MultiSellListContainer();
		temp.setListId(list.getListId());
		temp.setShowAll(list.getShowAll());
		temp.setNoTax(list.getNoTax());
		temp.setKeepEnchant(list.getKeepEnchant());
		temp.nokey = list.nokey;
		temp.community = list.community;

		for(MultiSellEntry e : list.getEntries())
		{
			if(temp.getEntries().size() == Config.MULTISELL_SIZE)
			{
				player.sendPacket(new MultiSellList(temp, page, 0));
				page++;
				temp = new MultiSellListContainer();
				temp.setListId(list.getListId());
			}
			temp.addEntry(e);
		}
		player.sendPacket(new MultiSellList(temp, page, 1));
	}

	private MultiSellListContainer generateMultiSell(int listId, L2Player player, double taxRate)
	{
		if(_handlers.containsKey(listId))
		{
			MultiSellListContainer list = _handlers.get(listId).generateMultiSellList(listId, player, taxRate);
			player.setLastMultisell(list);
			return list;
		}

		MultiSellListContainer list;
		GArray<MultiSellEntry> _possiblelist = new GArray<MultiSellEntry>();

		// Hardcoded  - обмен вещей на равноценные
		HashSet<L2ItemInstance> _items;
		// Все мультиселлы из датапака
		MultiSellListContainer _container = L2Multisell.getInstance().getList(listId);
		if(_container == null)
			return null;

		GArray<MultiSellEntry> _fulllist = _container.getEntries();
		boolean _enchant = _container.getKeepEnchant();
		final Inventory inv = player.getInventory();

		for(MultiSellEntry ent : _fulllist)
		{
			double tax = 0;

			// Обработка налога, если лист не безналоговый
			// Адены добавляются в лист если отсутствуют или прибавляются к существующим
			GArray<MultiSellIngredient> ingridients;
			if(!_container.getNoTax() && taxRate > 0.)
			{
				ingridients = new GArray<MultiSellIngredient>();
				for(MultiSellIngredient i : ent.getIngredients())
				{
					if(i.getItemId() == 57)
					{
						tax += i.getItemCount() * (taxRate + 1);
						continue;
					}
					ingridients.add(i);
					if(i.getItemId() == L2Item.ITEM_ID_CLAN_REPUTATION_SCORE || i.getItemId() == L2Item.ITEM_ID_FAME_POINTS || i.getItemId() == L2Item.ITEM_ID_PC_BANG_POINTS) //TODO: Проверить на корейском(?) оффе налог на банг поинты
					{
						// hardcoded. Налог на клановую репутацию. Формула проверена на с6 и соответсвует на 100%.
						tax += i.getItemCount() / 120 * 1000 * taxRate * 100;
						continue;
					}

					final L2Item item = ItemTable.getInstance().getTemplate(i.getItemId());
					if(item.isStackable())
						tax += item.getReferencePrice() * i.getItemCount() * taxRate;
				}

				if(tax >= 1)
					ingridients.add(new MultiSellIngredient(57, (int) tax));
			}
			else
				ingridients = ent.getIngredients();

			// Если стоит флаг "показывать все" не проверять наличие ингридиентов
			if(_container.getShowAll())
			{
				MultiSellEntry possibleEntry = new MultiSellEntry(ent.getEntryId());
				for(MultiSellIngredient p : ent.getProduction())
					possibleEntry.addProduct(p);
				for(MultiSellIngredient sn : ingridients)
					possibleEntry.addIngredient(sn);
				_possiblelist.add(possibleEntry);
			}
			else
			{
				HashSet<Integer> _itm = new HashSet<Integer>();
				// Проверка наличия у игрока ингридиентов
				boolean added = false;
				for(MultiSellIngredient mi : ingridients)
				{
					L2Item template = mi.getItemId() == L2Item.ITEM_ID_CLAN_REPUTATION_SCORE || mi.getItemId() == L2Item.ITEM_ID_FAME_POINTS || mi.getItemId() == L2Item.ITEM_ID_PC_BANG_POINTS ? null : ItemTable.getInstance().getTemplate(mi.getItemId());

					if(mi.getItemId() == L2Item.ITEM_ID_CLAN_REPUTATION_SCORE ||
							mi.getItemId() == L2Item.ITEM_ID_FAME_POINTS ||
							mi.getItemId() == L2Item.ITEM_ID_PC_BANG_POINTS ||
							template.getType2() <= L2Item.TYPE2_ACCESSORY ||
							template.getItemType() == L2EtcItem.EtcItemType.FOUNDATION) // Экипировка
					{
						//TODO: а мы должны тут сверять count?
						if(mi.getItemId() == L2Item.ITEM_ID_CLAN_REPUTATION_SCORE)
						{
							if(!_itm.contains(mi.getItemId()) && player.getClanId() != 0 && player.getClan().getReputationScore() >= mi.getItemCount())
								_itm.add(mi.getItemId());
							continue;
						}
						else if(mi.getItemId() == L2Item.ITEM_ID_PC_BANG_POINTS)
						{
							if(!_itm.contains(mi.getItemId()) && player.getPcBangPoints() >= mi.getItemCount())
								_itm.add(mi.getItemId());
							continue;
						}
						else if(mi.getItemId() == L2Item.ITEM_ID_FAME_POINTS)
						{
							if(!_itm.contains(mi.getItemId()) && player.getFame() >= mi.getItemCount())
								_itm.add(mi.getItemId());
							continue;
						}

						for(final L2ItemInstance item : inv.getItems())
							if(item.getItemId() == mi.getItemId() && !item.isEquipped() && (item.getCustomFlags() & L2ItemInstance.FLAG_NO_TRADE) != L2ItemInstance.FLAG_NO_TRADE)
							{
								if(_itm.contains(_enchant ? mi.getItemId() + mi.getItemEnchant() * 100000 : mi.getItemId())) // Не проверять одинаковые вещи
									continue;

								if(item.isStackable() && item.getCount() < mi.getItemCount())
									break;

								_itm.add(_enchant ? mi.getItemId() + mi.getItemEnchant() * 100000 : mi.getItemId());
								MultiSellEntry possibleEntry = new MultiSellEntry(_enchant ? ent.getEntryId() + item.getEnchantLevel() * 100000 : ent.getEntryId());

								for(MultiSellIngredient p : ent.getProduction())
									possibleEntry.addProduct(new MultiSellIngredient(p.getItemId(), p.getItemCount(), item));

								for(MultiSellIngredient ig : ingridients)
									if(template != null && template.getType2() <= L2Item.TYPE2_ACCESSORY && item.getEnchantLevel() > 0)
										possibleEntry.addIngredient(new MultiSellIngredient(ig.getItemId(), ig.getItemCount(), item));
									else
										possibleEntry.addIngredient(ig);

								added = true;
								_possiblelist.add(possibleEntry);
								break;
							}
					}
				}

				if(!added)
				{
					MultiSellIngredient mi = ingridients.get(0);
					L2ItemInstance item = inv.getItemByItemId(mi.getItemId());
					if(item != null && !item.isEquipped())
					{
						MultiSellEntry possibleEntry = new MultiSellEntry(_enchant ? ent.getEntryId() + item.getEnchantLevel() * 100000 : ent.getEntryId());
						for(MultiSellIngredient p : ent.getProduction())
							possibleEntry.addProduct(new MultiSellIngredient(p.getItemId(), p.getItemCount(), item));

						for(MultiSellIngredient ig : ingridients)
							if(item.getItem().getType2() <= L2Item.TYPE2_ACCESSORY && item.getEnchantLevel() > 0)
								possibleEntry.addIngredient(new MultiSellIngredient(ig.getItemId(), ig.getItemCount(), item));
							else
								possibleEntry.addIngredient(ig);

						_possiblelist.add(possibleEntry);
					}
				}
			}
		}

		list = new MultiSellListContainer();
		list.entries = _possiblelist;
		list.setListId(listId);
		list.setShowAll(_container.getShowAll());
		list.setNoTax(_container.getNoTax());
		list.setKeepEnchant(_container.getKeepEnchant());
		list.nokey = _container.nokey;
		list.community = _container.community;
		player.setLastMultisell(list);

		return list;
	}

	public void registerMultiSellHandler(MultiSellHandler handler)
	{
		for(int listId : handler.getMultiSellId())
		{
			if(_handlers.containsKey(listId))
				_log.warn("MultiSellHandler: " + listId + " already registered.");

			_handlers.put(listId, handler);
			_log.info("MultiSellHandler: handler for: " + listId + " list id registered.");
		}
	}

	public void unregisterMultiSellHandler(MultiSellHandler handler)
	{
		for(int listId : handler.getMultiSellId())
			_handlers.remove(listId);
	}
}