package ru.l2gw.gameserver.model.base;

/**
 * @author: rage
 * @date: 22.10.12 16:38
 */
public class ItemEnchantTemplate
{
	private final int itemId;
	private EnchantOption[][] options;

	public ItemEnchantTemplate(int itemId)
	{
		this.itemId = itemId;
	}

	public void addEnchantOptions(int enchantLevel, EnchantOption option)
	{
		if(options == null)
			options = new EnchantOption[enchantLevel + 1][];
		else
		{
			if(options.length <= enchantLevel)
			{
				EnchantOption[][] tmp = new EnchantOption[enchantLevel + 1][];
				System.arraycopy(options, 0, tmp, 0, options.length);
				options = tmp;
			}
		}

		if(options[enchantLevel] == null)
			options[enchantLevel] = new EnchantOption[]{ option };
		else
		{
			EnchantOption[] tmp = new EnchantOption[options[enchantLevel].length + 1];
			System.arraycopy(options[enchantLevel], 0, tmp, 0, options[enchantLevel].length);
			options[enchantLevel] = tmp;
			options[enchantLevel][options[enchantLevel].length - 1] = option;
		}
	}

	public EnchantOption[] getEnchantOption(int enchantLevel)
	{
		if(options == null || options.length < 1)
			return null;

		if(options.length <= enchantLevel)
			return options[Math.max(0, options.length - 1)];

		return options[enchantLevel];
	}

	public EnchantOption[][] getOptions()
	{
		return options;
	}
}
