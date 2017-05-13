package ru.l2gw.gameserver.serverpackets;

/**
 * @Author: rage
 * @Date: 4/10/2009 15:18:25
 */
public class ExRegenMax extends L2GameServerPacket
{
	private int _effectTime;
	private int _period;
	private double _hpReg; 

	public ExRegenMax(int effectTime, int period, double hpReg)
	{
		_effectTime = effectTime;
		_period = period;
		_hpReg = hpReg;
	}

	/**
	 * Пример пакета - Пришло после использования Healing Potion (инфа для Interlude, в Kamael пакет не изменился)
	 *
	 * FE 01 00 01 00 00 00 0F 00 00 00 03 00 00 00 00 00 00 00 00 00 38 40 // Healing Potion
	 * FE 01 00 01 00 00 00 0F 00 00 00 03 00 00 00 00 00 00 00 00 00 49 40 // Greater Healing Potion
	 * FE 01 00 01 00 00 00 0F 00 00 00 03 00 00 00 00 00 00 00 00 00 20 40 // Lesser Healing Potion
	 *
	 * FE - тип
	 * 01 00 - субтип
	 * 01 00 00 00 - хз что
	 * 0F 00 00 00 - Время эффекта
	 * 03 00 00 00 - Тики
	 * 00 00 00 00 00 00 38 40 - Сколько хп восстанавливается за Тики
	 */
	//dddf
	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x01);
		writeD(1);
		writeD(_effectTime);
		writeD(_period);
		writeF(_hpReg);
	}
}