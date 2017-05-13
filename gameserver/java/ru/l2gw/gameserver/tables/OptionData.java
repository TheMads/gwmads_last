package ru.l2gw.gameserver.tables;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.base.EnchantOption;
import ru.l2gw.gameserver.skills.DocumentOption;

import java.io.File;

/**
 * @author: rage
 * @date: 27.10.11 0:07
 */
public class OptionData
{
	private static final Log _log = LogFactory.getLog("options");
	private static EnchantOption[] options;

	public static void addEnchantOption(EnchantOption eo)
	{
		options[eo.getOptionId()] = eo;
	}

	public static void setMaxOptionsId(int maxId)
	{
		options = new EnchantOption[maxId + 1];
	}

	public static EnchantOption getEnchantOption(int optionId)
	{
		if(options.length > optionId)
			return options[optionId];

		return null;
	}

	public static void load()
	{
		try
		{
			File file = new File(Config.OPTIONDATA_FILE);

			if(!file.exists())
			{
				_log.info("OptionData: " + Config.OPTIONDATA_FILE + " file is missing.");
				return;
			}

			DocumentOption doc = new DocumentOption(file);
			doc.parse();

			_log.info("OptionData: loaded " + options.length + " enchant options.");
		}
		catch(Exception e)
		{
			_log.warn("OptionData: error while option data" + e);
			e.printStackTrace();
		}
	}
}
