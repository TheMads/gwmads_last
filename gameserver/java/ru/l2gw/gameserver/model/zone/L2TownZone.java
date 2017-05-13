package ru.l2gw.gameserver.model.zone;

import ru.l2gw.gameserver.instancemanager.TownManager;

public class L2TownZone extends L2DefaultZone
{
	private int _redirectTownId;

	public L2TownZone()
	{
		super();
		// Default to Giran
		_redirectTownId = 9;
		_zoneTypes.add(ZoneType.town);
	}

	@Override
	public void setAttribute(String name, String value)
	{
		if(name.equals("redirectTownId"))
			_redirectTownId = Integer.parseInt(value);
		else super.setAttribute(name, value);
	}

	@Override
	public void register()
	{
		TownManager.getInstance().addZone(this);
	}

	public int getRedirectTownId()
	{
		return _redirectTownId;
	}
}
