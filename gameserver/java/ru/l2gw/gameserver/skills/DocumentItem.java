package ru.l2gw.gameserver.skills;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.l2gw.extensions.listeners.items.ItemEquipListener;
import ru.l2gw.extensions.listeners.items.UnequipDispelListener;
import ru.l2gw.gameserver.skills.conditions.Condition;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.templates.L2Item;

import java.io.File;

/** 
 * remake by agr0naft
**/
final class DocumentItem extends DocumentBase
{
	/**
	 * @param itemData
	 * @param f
	 */
	public DocumentItem(File file)
	{
		super(file);
	}

	@Override
	protected Object getTableValue(String name)
	{
		return null;
	}

	@Override
	protected Object getTableValue(String name, int idx)
	{
		return null;
	}

	@Override
	protected void parseDocument(Document doc)
	{
		for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			if("list".equalsIgnoreCase(n.getNodeName()))
			{
				for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					if("item".equalsIgnoreCase(d.getNodeName()))
						parseItem(d);
			}
			else if("item".equalsIgnoreCase(n.getNodeName()))
				parseItem(n);
	}

	protected void parseItem(Node n)
	{
		int itemId = Integer.parseInt(n.getAttributes().getNamedItem("id").getNodeValue());
		String itemName = n.getAttributes().getNamedItem("name").getNodeValue();

		L2Item currentItem = ItemTable.getInstance().getTemplate(itemId);
		if(currentItem == null)
		{
			_log.warn("Item " + itemId + "(" + itemName + ") not found in database!");
			return;
		}

		Node first = n.getFirstChild();
		for(n = first; n != null; n = n.getNextSibling())
			if("for".equalsIgnoreCase(n.getNodeName()))
				parseTemplate(n, currentItem);

		for(n = first; n != null; n = n.getNextSibling())
			if("cond".equalsIgnoreCase(n.getNodeName()))
			{
				Condition condition = parseCondition(n.getFirstChild(), currentItem);
				if(condition != null)
				{
					Node msg = n.getAttributes().getNamedItem("msg");
					Node msgId = n.getAttributes().getNamedItem("msgId");

					if(msg != null)
						condition.setMessage(msg.getNodeValue());
					else if(msgId != null)
						condition.setMessageId(Integer.parseInt(msgId.getNodeValue()));

					currentItem.attachCondition(condition);
				}
			}

		for(n = first; n != null; n = n.getNextSibling())
			if("listener".equalsIgnoreCase(n.getNodeName()))
				for(Node l = n.getFirstChild(); l != null; l = l.getNextSibling())
				{
					ItemEquipListener listener = parseItemListener(l);
					if(listener != null)
						currentItem.attachEquipListener(listener);
				}
	}

	private ItemEquipListener parseItemListener(Node l)
	{
		if("unequipDispel".equalsIgnoreCase(l.getNodeName()))
			return new UnequipDispelListener(l.getAttributes().getNamedItem("abnormal").getNodeValue());

		return null;
	}
}