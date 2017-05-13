package ru.l2gw.gameserver.model.zone;

import ru.l2gw.gameserver.instancemanager.boss.AntharasManager;
import ru.l2gw.gameserver.instancemanager.boss.BaiumManager;
import ru.l2gw.gameserver.instancemanager.boss.ValakasManager;

/**
 * Created by IntelliJ IDEA.
 * User: admin
 * Date: 20.01.2009
 * Time: 13:51:17
 */
public class L2BossZone extends L2DefaultZone
{
	private String _boss = "";

	public L2BossZone()
	{
		super();
	}

	@Override
	public void setAttribute(String name, String value)
	{
		if(name.equals("boss"))
			_boss = value;
		else
			super.setAttribute(name, value);
	}

	public void register()
	{
		if(_boss.equalsIgnoreCase("Antharas"))
			AntharasManager.getInstance().registerZone(this);
		else if(_boss.equalsIgnoreCase("Valakas"))
			ValakasManager.getInstance().registerZone(this);
		else if(_boss.equalsIgnoreCase("Baium"))
			BaiumManager.getInstance().registerZone(this);
	}
}
