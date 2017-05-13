package ru.l2gw.gameserver.model.gmaccess.limits;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Territory;
import ru.l2gw.gameserver.model.gmaccess.IAdminLimit;
import ru.l2gw.gameserver.tables.SpawnTable;

import java.util.regex.Matcher;

/**
 * @author: rage
 * @date: 10.03.12 11:41
 */
public class RangeLimit implements IAdminLimit
{
	private final L2Territory range;
	private final boolean checkTarget;

	public RangeLimit(String points, boolean checkTarget)
	{
		range = new L2Territory("range-limit");
		this.checkTarget = checkTarget;
		Matcher m = SpawnTable.tp.matcher(points);

		while(m.find())
			range.add(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
	}

	public boolean checkLimit(L2Player player, L2Character target, Object arg1, Object arg2, Object arg3)
	{
		return player == null || (range.isInside(player.getX(), player.getY(), player.getZ()) && (!checkTarget || target == null || range.isInside(target.getX(), target.getY(), target.getZ())));
	}
}
