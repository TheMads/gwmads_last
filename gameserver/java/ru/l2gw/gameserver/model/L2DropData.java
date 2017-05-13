package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.base.ItemToDrop;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.templates.L2EtcItem;
import ru.l2gw.gameserver.templates.L2Item;

public class L2DropData
{
	private final L2Item _item;
	private final int _minBase;
	private final int _maxBase;
	private final int _chanceBase;
	private int _minBalanced;
	private int _maxBalanced;
	private int _chanceBalanced;
	private final int _groupId;
	private final boolean _isRaid;
	private boolean _fixedQty;

	public L2DropData(L2Item item, int min, int max, int chance, int gid)
	{
		_item = item;
		_minBase = min;
		_maxBase = max;
		_chanceBase = chance;
		_minBalanced = min;
		_maxBalanced = max;
		_chanceBalanced = chance;
		_groupId = gid;
		_isRaid = false;
		_fixedQty = _item.getItemClass() == L2ItemInstance.ItemClass.SPELLBOOKS || _item.type == L2EtcItem.EtcItemType.ARROW || _item.type == L2EtcItem.EtcItemType.BOLT || _item.type == L2EtcItem.EtcItemType.HERB;
	}

	public L2DropData(L2Item item, int min, int max, int chance, int gid, boolean isRaid)
	{
		_item = item;
		_minBase = min;
		_maxBase = max;
		_chanceBase = chance;
		_minBalanced = min;
		_maxBalanced = max;
		_chanceBalanced = chance;
		_groupId = gid;
		_isRaid = isRaid;
		_fixedQty = _item.getItemClass() == L2ItemInstance.ItemClass.SPELLBOOKS || _item.type == L2EtcItem.EtcItemType.ARROW || _item.type == L2EtcItem.EtcItemType.BOLT || _item.type == L2EtcItem.EtcItemType.HERB;
	}

	public boolean isFixedQty()
	{
		return _fixedQty;
	}

	public short getItemId()
	{
		return _item.getItemId();
	}

	public L2Item getItem()
	{
		return _item;
	}

	public int getGroupId()
	{
		return _groupId;
	}

	public int getMinDrop()
	{
		return _minBase;
	}

	public int getMaxDrop()
	{
		return _maxBase;
	}

	public int getChance()
	{
		return _chanceBase;
	}

	public void setBalanced(int chance, int min, int max)
	{
		_chanceBalanced = chance;
		_minBalanced = min;
		_maxBalanced = max;
	}

	public int getBalancedChance()
	{
		return _chanceBalanced;
	}

	public int getBalancedMin()
	{
		return _minBalanced;
	}

	public int getBalancedMax()
	{
		return _maxBalanced;
	}

	public boolean isRaid()
	{
		return _isRaid;
	}

	@Override
	public String toString()
	{
		return "ItemID: " + getItemId() + " Min: " + getMinDrop() + " Max: " + getMaxDrop() + " Chance: " + getChance() / 10000.0 + "%";
	}

	/**
	 * Подсчет шанса выпадения этой конкретной вещи
	 * Используется в эвентах и некоторых специальных механизмах
	 * @param player игрок (его бонус влияет на шанс)
	 * @param mod (просто множитель шанса)
	 * @return информация о выпавшей вещи
	 */
	public ItemToDrop roll(L2Player player, double mod)
	{
		float rate = Config.RATE_DROP_ITEMS;
		float adenarate = Config.RATE_DROP_ADENA;

		// calc group chance
		double calcChance = mod * _chanceBase * (_item.isAdena() ? 1f : rate);

		int dropmult = 1;
		// Если шанс оказался больше 100%
		if(calcChance > L2Drop.MAX_CHANCE)
			if(calcChance % L2Drop.MAX_CHANCE == 0) // если кратен 100% то тупо умножаем количество
				dropmult = (int) (calcChance / L2Drop.MAX_CHANCE);
			else
			{ // иначе балансируем
				dropmult = (int) Math.ceil(calcChance / L2Drop.MAX_CHANCE); // множитель равен шанс / 100% округление вверх
				calcChance = calcChance / dropmult; // шанс равен шанс / множитель
				// в результате получаем увеличение количества и уменьшение шанса, при этом шанс не падает ниже 50%
			}

		if(Rnd.get(L2Drop.MAX_CHANCE) > calcChance)
			return null;

		ItemToDrop t = new ItemToDrop(_item.getItemId());

		// если это адена то умножаем на рейт адены, иначе на множитель перебора шанса
		float mult = _item.isAdena() ? adenarate : dropmult;

		if(getMinDrop() >= getMaxDrop())
			t.count = (long) (getMinDrop() * mult);
		else
			t.count = Rnd.get((long) (getMinDrop() * mult), (long) (getMaxDrop() * mult));

		return t;
	}

	@Override
	public int hashCode()
	{
		return _item.getItemId();
	}
}