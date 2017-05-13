package ru.l2gw.gameserver.tables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ru.l2gw.commons.arrays.ArrayUtils;
import ru.l2gw.commons.utils.XmlUtil;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.base.EnchantOption;
import ru.l2gw.gameserver.model.base.ItemEnchantTemplate;
import ru.l2gw.gameserver.templates.L2Item;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * @author: rage
 * @date: 22.10.12 16:50
 */
public class EnchantOptionTable
{
	private static final Log log = LogFactory.getLog(EnchantOptionTable.class.getSimpleName());

	public static void load()
	{
		try
		{
			File file = new File(Config.ENCHANTOPTION_FILE);

			if(!file.exists())
			{
				log.info("EnchantOption: " + Config.ENCHANTOPTION_FILE + " file is missing.");
				return;
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);

			Document doc = factory.newDocumentBuilder().parse(file);
			int optionsCount = 0;
			int items = 0;
			for(Node n = doc.getFirstChild(); n != null; n = n.getNextSibling())
			{
				try
				{
					if("enchantoptions".equalsIgnoreCase(n.getNodeName()))
					{
						for(Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
						{
							if("enchantoption".equalsIgnoreCase(d.getNodeName()))
							{
								int itemId = XmlUtil.getIntAttribute(d, "itemId");
								ItemEnchantTemplate template = new ItemEnchantTemplate(itemId);

								for(Node o = d.getFirstChild(); o != null; o = o.getNextSibling())
								{
									if("enchant".equals(o.getNodeName()))
									{
										int enchant = XmlUtil.getIntAttribute(o, "level");
										int options[] = ArrayUtils.toIntArray(XmlUtil.getAttribute(o, "options"));
										for(int optionId : options)
										{
											EnchantOption eo = OptionData.getEnchantOption(optionId);
											if(eo == null)
											{
												log.warn("EnchantOption: no option id: " + optionId + " for item id: " + itemId + " enchant level: " + enchant);
												continue;
											}
											template.addEnchantOptions(enchant, eo);
											optionsCount++;
										}
									}
								}
								items++;

								L2Item item = ItemTable.getInstance().getTemplate(itemId);
								if(item != null)
									item.addEnchantOptions(template);
							}
						}
					}
				}
				catch(Exception e)
				{
					log.warn("EnchantOption: can't load " + Config.ENCHANTOPTION_FILE + " " + e, e);
				}
			}

			log.info("EnchantOption: loaded " + optionsCount + " for " + items + " items.");
		}
		catch(Exception e)
		{
			log.warn("EnchantOption: can't load " + Config.ENCHANTOPTION_FILE + " " + e, e);
		}
	}
}
