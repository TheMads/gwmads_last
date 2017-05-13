package ru.l2gw.gameserver.model;

public class L2KamaelWeaponExchange
{
	private int _originalId;
	private int _kamaelId;

	public void setOriginal(int originalId)
	{
		_originalId = originalId;
	}

	public void setKamael(int kamaelId)
	{
		_kamaelId = kamaelId;
	}

	public int getOriginal()
	{
		return _originalId;
	}

	public int getKamael()
	{
		return _kamaelId;
	}
}