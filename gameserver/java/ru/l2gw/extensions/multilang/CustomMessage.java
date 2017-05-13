package ru.l2gw.extensions.multilang;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2Item;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Даный класс является обработчиком интернациональных сообщений.
 * Поддержживается полностью юникод.
 *
 * По функциональности он не уступает SystemMessage, но поддерживает одновременно несколько языков.
 *
 * @Author: Death
 * @Date: 10/6/2007
 * @Time: 10:34:57
 */
public class CustomMessage
{
	private static String localizationDirSrc = "data/localization/";
	private static String localizationDirASCII = "data/localization/ascii/";

	private static URLClassLoader loader;

	static
	{
		File src = new File(localizationDirSrc);

		for(File prop : src.listFiles(new PropertiesFilter()))
			ASCIIBuilder.createPropASCII(prop);

		try
		{
			loader = new URLClassLoader(new URL[] { new File(localizationDirASCII).toURI().toURL() });
		}
		catch(MalformedURLException e)
		{
			e.printStackTrace(System.err);
		}
	}

	private String _text;
	private int mark = 0;

	/**
	 * Создает новый инстанс сообщения.
	 * @param address адрес(ключ) параметра с языком интернационализации
	 * @param player игрок у которого будет взят язык
	 */
	public CustomMessage(String address, L2Object player)
	{
		if(player != null && player.isPlayer())
			getString(address, ((L2Player) player).getVar("lang@"));
		else
			getString(address, Config.DEFAULT_LANG);
	}

	/**
	 * Создает новый инстанс сообщения
	 * @param address адрес(ключ) параметра с языком интернационализации
	 * @param language язык по которому будет взято сообщение
	 */
	public CustomMessage(String address, String language)
	{
		getString(address, language);
	}

	private static final Locale en = new Locale("en");

	private void getString(String address, String lang)
	{
		if(lang != null)
			lang = lang.toLowerCase();
		else
			lang = "en";

		ResourceBundle rb;

		try
		{
			rb = ResourceBundle.getBundle("messages", new Locale(lang), loader);
		}
		catch(Exception e)
		{
			rb = ResourceBundle.getBundle("messages", en, loader);
		}

		try
		{
			_text = rb.getString(address);
		}
		catch(Exception e)
		{
			_text = "Custom message with address: \"" + address + "\" is unsupported!";
		}
	}

	/**
	 * Заменяет следующий елемент числом.<br>
	 * {0} {1} ... {Integer.MAX_VALUE}
	 * @param number чем мы хотим заменить
	 * @return этот инстанс уже с имененным текстом
	 */
	public CustomMessage addNumber(long number)
	{
		_text = _text.replace("{" + mark + "}", String.valueOf(number));
		mark++;
		return this;
	}

	/**
	 * Заменяет следующий елемент строкой.<br>
	 * {0} {1} ... {Integer.MAX_VALUE}
	 * @param str чем мы хотим заменить
	 * @return этот инстанс уже с имененным текстом
	 */
	public CustomMessage addString(String str)
	{
		_text = _text.replace("{" + mark + "}", str);
		mark++;
		return this;
	}

	/**
	 * Заменяет следующий елемент именем скилла.<br>
	 * {0} {1} ... {Integer.MAX_VALUE}
	 * @param skill именем которого мы хотим заменить.
	 * @return этот инстанс уже с имененным текстом
	 */
	public CustomMessage addSkillName(L2Skill skill)
	{
		_text = _text.replace("{" + mark + "}", skill.getName());
		mark++;
		return this;
	}

	/**
	 * Заменяет следующий елемент именем скилла.<br>
	 * {0} {1} ... {Integer.MAX_VALUE}
	 * @param skillId именем которого мы хотим заменить.
	 * @param skillLevel уровень скилла
	 * @return этот инстанс уже с имененным текстом
	 */
	public CustomMessage addSkillName(short skillId, short skillLevel)
	{
		return addSkillName(SkillTable.getInstance().getInfo(skillId, skillLevel));
	}

	/**
	 * Заменяет следующий елемент именем предмета.<br>
	 * {0} {1} ... {Integer.MAX_VALUE}
	 * @param item именем которого мы хотим заменить.
	 * @return этот инстанс уже с имененным текстом
	 */
	public CustomMessage addItemName(L2Item item)
	{
		_text = _text.replace("{" + mark + "}", item.getName());
		mark++;
		return this;
	}

	/**
	 * Заменяет следующий елемент именем предмета.<br>
	 * {0} {1} ... {Integer.MAX_VALUE}
	 * @param itemId именем которого мы хотим заменить.
	 * @return этот инстанс уже с имененным текстом
	 */
	public CustomMessage addItemName(int itemId)
	{
		return addItemName(ItemTable.getInstance().getTemplate(itemId));
	}

	/**
	 * Заменяет следующий елемент именем предмета.<br>
	 * {0} {1} ... {Integer.MAX_VALUE}
	 * @param item именем которого мы хотим заменить.
	 * @return этот инстанс уже с имененным текстом
	 */
	public CustomMessage addItemName(L2ItemInstance item)
	{
		return addItemName(item.getItem());
	}

	/**
	 * Заменяет следующий елемент именем персонажа.<br>
	 * {0} {1} ... {Integer.MAX_VALUE}
	 * @param cha именем которого мы хотим заменить.
	 * @return этот инстанс уже с имененным текстом
	 */
	public CustomMessage addCharName(L2Character cha)
	{
		_text = _text.replace("{" + mark + "}", cha.getName());
		mark++;
		return this;
	}

	/**
	 * Возвращает локализированную строку, полученную после всех действий.
	 * @return строка.
	 */
	@Override
	public String toString()
	{
		return _text;
	}
}
