package ru.l2gw.gameserver.skills;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.base.EnchantOption;
import ru.l2gw.gameserver.tables.OptionData;
import ru.l2gw.gameserver.tables.SkillTable;

import java.io.File;

/**
 * @author: rage
 * @date: 27.10.11 0:18
 */
public class DocumentOption extends DocumentBase
{
	public DocumentOption(File file)
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
		int maxId = 0;
		for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			if("optiondata".equalsIgnoreCase(n.getNodeName()))
			{
				for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					if("option".equalsIgnoreCase(d.getNodeName()))
					{
						int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
						if(id > maxId)
							maxId = id;
					}
			}

		OptionData.setMaxOptionsId(maxId);

		for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			if("optiondata".equalsIgnoreCase(n.getNodeName()))
			{
				for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
					if("option".equalsIgnoreCase(d.getNodeName()))
						parseOption(d);
			}
	}

	protected void parseOption(Node n)
	{
		NamedNodeMap attr = n.getAttributes();
		int optionId = Integer.parseInt(attr.getNamedItem("id").getNodeValue());
		L2Skill skill = attr.getNamedItem("passive_skill") != null ? SkillTable.parseSkillInfo(attr.getNamedItem("passive_skill").getNodeValue()) : null;
		if(skill == null)
			skill = attr.getNamedItem("active_skill") != null ? SkillTable.parseSkillInfo(attr.getNamedItem("active_skill").getNodeValue()) : null;

		EnchantOption eo = new EnchantOption(optionId, skill);
		parseTemplate(n, eo);
		OptionData.addEnchantOption(eo);
	}
}
