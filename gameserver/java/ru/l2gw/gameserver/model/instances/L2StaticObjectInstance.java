package ru.l2gw.gameserver.model.instances;

import ru.l2gw.extensions.scripts.Events;
import ru.l2gw.gameserver.ai.CtrlIntention;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.serverpackets.MyTargetSelected;
import ru.l2gw.gameserver.serverpackets.NpcHtmlMessage;
import ru.l2gw.gameserver.serverpackets.ShowTownMap;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

public class L2StaticObjectInstance extends L2NpcInstance
{
	private int _staticObjectId;
	private int _type = -1; // 0 - signs, 1 - throne, 2 - starter town map, 3 - flagpole
	private String _filePath;
	private int _mapX;
	private int _mapY;

	public int getStaticObjectId()
	{
		return _staticObjectId;
	}

	public void setStaticObjectId(int StaticObjectId)
	{
		_staticObjectId = StaticObjectId;
	}

	public L2StaticObjectInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	public int getType()
	{
		return _type;
	}

	public void setType(int type)
	{
		_type = type;
	}

	public void setFilePath(String path)
	{
		_filePath = path;
	}

	public void setMapX(int x)
	{
		_mapX = x;
	}

	public void setMapY(int y)
	{
		_mapY = y;
	}

	@Override
	public void onAction(L2Player player, boolean dontMove)
	{
		if(!dontMove && Events.onAction(player, this))
			return;
		else if(dontMove && Events.onActionShift(player, this))
			return;

		if(player.getTarget() != this)
		{
			if(player.setTarget(this))
				player.sendPacket(new MyTargetSelected(getObjectId(), 0));
			return;
		}

		//MyTargetSelected my = new MyTargetSelected(getObjectId(), 0);
		//player.sendPacket(my);

		if(!isInRange(player, getInteractDistance(player)))
		{
			if(player.getAI().getIntention() != CtrlIntention.AI_INTENTION_INTERACT && !dontMove)
				player.getAI().setIntention(CtrlIntention.AI_INTENTION_INTERACT, this, null);
			else
				player.sendActionFailed();
			return;
		}

		if(_type == 0) // Arena Board
			player.sendPacket(new NpcHtmlMessage(player, this, "data/html/newspaper/arena.htm", 0));
		else if(_type == 2) // Village map
		{
			player.sendPacket(new ShowTownMap(_filePath, _mapX, _mapY));
			player.sendActionFailed();
		}
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return false;
	}

	@Override
	public void doDie(L2Character killer)
	{}

	@Override
	public boolean isInvul()
	{
		return true;
	}

	@Override
	public boolean isFearImmune()
	{
		return true;
	}

	@Override
	public boolean isLethalImmune()
	{
		return true;
	}

	public L2Skill.TargetType getTargetRelation(L2Character target, boolean offensive)
	{
		return getType() == 3 ? L2Skill.TargetType.flagpole : L2Skill.TargetType.none;
	}
}