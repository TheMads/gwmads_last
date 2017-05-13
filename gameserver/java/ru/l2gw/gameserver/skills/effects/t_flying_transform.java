package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.util.Location;

/**
 * @author rage
 * @date 19.08.2010 14:32:14
 */
public class t_flying_transform extends t_effect
{
	private final int _maleNpcId, _femaleNpcId;

	public t_flying_transform(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
		_maleNpcId = template._attrs.getInteger("npcId", 0);
		_femaleNpcId = template._attrs.getInteger("femaleNpcId", _maleNpcId);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(!getEffected().isPlayer())
			return;

		if(getEffected().getX() > -166168)
		{
			_effect.exit();
			return;
		}

		L2Player player = (L2Player) getEffected();
		if(player.getTransformation() > 0)
			return;

		int id = (int) calc();
		for(L2Effect effect : player.getAllEffectsArray())
			if(effect.getSkill().isToggle())
				effect.exit();

		player.decayMe();
		player.setFlying(true);
		player.setXYZ(player.getX(), player.getY(), player.getZ() + 32, false);
		player.setTransformationTemplate(player.getSex() == 0 ? _maleNpcId : _femaleNpcId);
		player.setTransformation(id);
		player.spawnMe();
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(!getEffected().isPlayer())
			return;

		L2Player player = (L2Player) getEffected();
		if(player.getTransformation() == 0)
			return;

		player.decayMe();
		player.setFlying(false);
		Location loc = player.getLoc().correctGeoZ();
		player.setXYZ(loc.getX(), loc.getY(), loc.getZ(), false);
		player.setTransformation(0);
		player.setTransformationTemplate(0);
		player.spawnMe();
	}
}
